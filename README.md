# Machine Learning for Software refactoring

This repository contains all our exploration on the use
of machine learning methods to recommend software refactoring.

It currently contains the following projects:

* `data-collection`: The java tool that collects refactoring and non-refactorings instances that are later used to train the ML algorithms.

* `data-collection-scripts`: A bunch of bash scripts that we use to deploy our data collection tool in several machines at the same time. Probably not useful to other people, but us. 

* `machine-learning`: The python scripts that train the different ML algorithms.

## Paper and appendix 

* The raw dataset can be found here: https://zenodo.org/record/3547639
* The appendix with our full results can be found here: https://zenodo.org/record/3583980
* The paper can be found here: TODO 


## The data collection tool

You can run the data collection by simply running the `App.java` class. This class contains a program that requires the following parameters, in this order:

1. _The name dataset_: A hard-coded string with the name of the dataset (e.g., "apache", "fdroid"). This information appears in the generated data later on, so that you can use it as a filter.

1. _The git URL_: The git url of the project to be analyzed. Your local machine must have all the permissions to clone it (i.e., _git clone url_ should work). Cloning will happen in a temporary directory.

1. _Output path_: The directory where the tool is going to store the source code before and after the refactoring. This step is important if you plan to do later analysis on the refactored files. The directory structure basically contains the hash of the refactoring, as well as the file before and after. The name of the file also contains the refactoring it suffered, to facilitate parsing. For more details on the name of the file, see our implementation.

1. _Database URL_: JDBC URL that points to your MySQL. The database must exist and be empty. The tool will create the required tables.

1. _Database user_: Database user.

1. _Database password_: Database password. 

1. _K threshold_: Threshold used to determine non-refactoring instances.

These parameters can be passed via command-line, if you exported a JAR file. 
Example:

```
java -jar refactoring.jar <dataset> <git-url> <output-path> <database-url> <database-user> <database-password> <k-threshold>
```

### Configuring the tool

Use Maven: `mvn clean compile`. Or just import it via IntelliJ; it will know what to do.

If you want to export a jar file and run it somewhere else, just do `mvn clean package`. A .jar file will be created under the `target/` folder.


### Cleaning up the final database

When running in scale, e.g., in thousands of projects, some projects will definitely fail (e.g., maybe the AST parser fails, etc etc). Although the app tries to remove problematic rows, if the process dies completely (maybe out of memory in your machine), partially loaded projects will be in the database. 

We use the following queries to remove half-baked projects completely from our database:

```
delete from yes where project_id in (select id from project where finishedDate is null);
delete from no where project_id in (select id from project where finishedDate is null);
delete from project where finishedDate is null;
```

## The machine learning pipeline

This project contains all the Python scripts that are responsible
for the ML pipeline.

### Installing and configuring the database.

First, install all the dependencies:

```
pip3 install --user -r requirements.txt
```

Then, create a `config.ini` file, following the example structure in
`config-example.ini`. In this file, you configure your database connection.

### Training and testing models

The main Python script that generates all the models and results is the
`main_generate_models.py`. You run it by simply calling `python3 main_generate_models.py`.

The script will follow the configurations in the `configs.py`. There, you can define which datasets to analyze, which models to build, which under sampling algorithms to use, and etc. Please, read the comments of this file.

For this script to run, you need to create a `results/` folder inside the
`machine-learning` folder. The results will be stored there.

The generated output is a text file with a weak structure. A quick way to get results is by grepping:

* `cat *.txt | grep "CSV"`: returns a CSV with all the models and their precision, recall, and accuracy.
* `cat *.txt | grep "TIME"`: returns a CSV with how much time it took to train and test the model.


Before running the pipeline, we suggest you to warm up the cache. The warming up basically executes all the required queries and cache them as CSV files. These queries can take a long time to run... and if you are like us, you will most likely re-execute your experiments many times! :) Thus, having them cached helps:

```
python3 warm_cache.py
```

If you need to clean up the cache, simply delete the `_cache` directory that is created under the `machine-learning` folder.


### Cross-domain validation

The `main_cross_models_evaluation.py` performs pairwise comparisons between
two different datasets. In other words, it tests all models that were trained using a given dataset A in the data of a given dataset B. For example, it tests how Apache models behave in F-Droid models.

For this script to run, you need to create a `results/` folder inside the
`machine-learning` folder. The final CSV will be stored there. The header of the
CSV is clear.


### Other scripts

The `main_parse_*` scripts help you in parsing the resulting text file, and extract the best hyper parameters, the feature importance, and the individual k-fold results. (These are always in maintenance, as they are highly sensitive to the format that the main tool generates logs.)

## Authors

This project was initially envisioned by Maurício Aniche, Erick Galante Maziero, Rafael Durelli, and Vinicius Durelli.

## License

This project is licensed under the MIT license.
