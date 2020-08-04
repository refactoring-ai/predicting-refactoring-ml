import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from db.DBConnector import execute_query
from statistics.plot_utils import heatmap
from utils.log import log_init, log_close, log
import time
from os import path


def compute_probability(dataframe, divisor, file_addition: str):
    """
    Compute the likelihood of a refactoring to occur in its row and assert if the results are valid.

    Parameters
    ----------
    dataframe
        Compute the likelihood of these refactoring types
    divisor
        Use this to compute the likelihood for each row
    file_addition
        Store the results in this file.
    """
    #compute the probabilty of a refatoring co-occuring on the same commit
    dataframe_probability = dataframe.div(divisor)

    #set the diagonal to zero, in order to clean up the matrix
    np.testing.assert_array_equal(np.diag(dataframe_probability), 1.0, err_msg="Not all elements on the diagonal are zero.")
    np.fill_diagonal(dataframe_probability.values, 0)
    dataframe_probability.to_csv("results/Refactorings_%s_probability.csv" % file_addition, index=False)
    print("Co-occurrence likelihood matrix with shape: " + str(dataframe_probability.shape) + " was computed and stored.")
    return dataframe_probability


def filter_probability(dataframe, threshold, labels, file_addition: str):
    """
    Filter the rows and columns of this dataframe to have at least the specified likelihhod.

    Parameters
    ----------
    dataframe
        Filter the rows and columns of this diagonal matrix.
    threshold
        Filter the max likelihood of each row and column with threshold.
    labels
        Labels fitting the rows and columns of the diagonal matrix.
    """
    filtered_rows_matrix, filtered_labels_rows, filtered_labels_columns, drop_columns = [], [], [], []
    #filter all columns
    for name, values in dataframe.iteritems():
        if values.max() <= threshold:
            drop_columns.append(name)
        else:
            filtered_labels_columns.append(name)
    dataframe_probality_filtered = dataframe.drop(drop_columns, axis=1)
    #filter all rows
    for index, row in dataframe_probality_filtered.iterrows():
        if row.max() > threshold:
            filtered_rows_matrix.append(row)
            filtered_labels_rows.append(labels[index])
    co_occurrence_matrix = pd.DataFrame(filtered_rows_matrix, columns=dataframe_probality_filtered.columns)
    co_occurrence_matrix.to_csv("results/Refactorings_%s_probability_%s.csv" % (file_addition, threshold), index=False)
    print("Co-occurrence likelihood matrix with shape: " + str(co_occurrence_matrix.shape) +
          " and threshold: " + str(threshold) + " was computed and  stored.")
    return co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns


def co_occurence_commit(refactorings, probability_threshold = 0.0):
    """
    Computes the occurrence probability of refactorings occurring on the same commit.
    1. It retrieves the relevant data from the refactoringspercommit table.
    2. Processes the data, assert validity, remove diagonal, compute probability per commit
        and filters with the probability threshold
    3. Stores the intermediate results in csv files
    4. Plots and stores a heatmap of the resulting matrix

    Parameters
    ----------
    refactorings
        all relevant refactoring types to consider for the matrix
    probability_threshold
        the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
    """
    #get the raw data
    dataframe = pd.DataFrame()
    if not path.exists("results/Refactorings_commit_statistics.csv"):
        for refactoring_name in refactorings:
            query = "SELECT "
            query += ", ".join(["SUM(IF(`%s count` > 0, 1, 0)) AS `%s`" % (refactoring_type, refactoring_type) for refactoring_type in refactorings])
            query += ", COUNT(commitMetaData_id) AS `Commit Count` "
            query += ("FROM refactoringspercommit WHERE `%s count` > 0" % refactoring_name)
            refactoringspercommit = execute_query(query)
            refactoringspercommit["Refactoring Type"] = refactoring_name
            dataframe = pd.concat([dataframe, refactoringspercommit])
        #store the dataframe to have the raw data
        dataframe.to_csv("results/Refactorings_commit_statistics.csv", index=False)
        print("Got the raw data from refactoringspercommit and stored it in: results/Refactorings_commit_statistics.csv.")
    else:
        dataframe = pd.read_csv("results/Refactorings_commit_statistics.csv")

    # extract the labels for the plot and remove unplotted data
    labels = dataframe["Refactoring Type"]
    commit_count = dataframe["Commit Count"].values
    dataframe = dataframe.drop(["Refactoring Type", "Commit Count"], axis=1)

    #compute the probabilities
    dataframe_probability = compute_probability(dataframe, commit_count, "commit")

    #drop irrelevant rows, to simplify the plot
    co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns = filter_probability(dataframe_probability, probability_threshold, labels, "commit")

    #plot the matrix
    #create a subplot, in order to name the columns and rows
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=160)
    im, cbar = heatmap(co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel="Co-occurence [P/ Commit]")
    plt.title("Co-occurrence of refactoring types on the same commit (min[row | col] > %s)" % probability_threshold)
    plt.savefig("results/Refactorings_co-occurrence_commit_likelihood_%s_%s.png" % (probability_threshold, str(co_occurrence_matrix.shape)))
    print("Saved figure: Co-occurrence of refactoring types on the same commit (min[row | col] > %s)" % probability_threshold)


def co_occurence_window(refactorings, table: str = "6H", probability_threshold = 0.0):
    """
    Computes the occurrence probability of refactorings occurring on the same commit.
    1. It retrieves the relevant data from the refactoringspercommit table.
    2. Processes the data, assert validity, remove diagonal, compute probability per commit
        and filters with the probability threshold
    3. Stores the intermediate results in csv files
    4. Plots and stores a heatmap of the resulting matrix

    Parameters
    ----------
    refactorings
        all relevant refactoring types to consider for the matrix
    probability_threshold
        the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
    """
    dataframe = pd.DataFrame()
    if not path.exists("results/Refactorings_window_%s_statistics.csv" % table):
        for refactoring_name in refactorings:
            query = "SELECT "
            query += ", ".join(["SUM(IF(`%s count` > 0, 1, 0)) AS `%s`" % (refactoring_type, refactoring_type) for refactoring_type in refactorings])
            query += ",COUNT(*) AS `Window Count` "
            query += ",SUM(`Commit Count`) AS `Window Size Total` "
            query += ("FROM RefactoringsWindow_%s WHERE `%s count` > 0" % (table, refactoring_name))
            refactoringspercommit = execute_query(query)
            refactoringspercommit["Refactoring Type"] = refactoring_name
            dataframe = pd.concat([dataframe, refactoringspercommit])
        #store the dataframe to have the raw data
        dataframe.to_csv("results/Refactorings_window_%s_statistics.csv" % table, index=False)
        print("Got the raw data from Refactorings_window_%s and stored it in: results/Refactorings_commit_statistics.csv." % table)
    else:
        dataframe = pd.read_csv("results/Refactorings_window_%s_statistics.csv" % table)

    # extract the labels for the plot and remove unplotted data
    labels = dataframe["Refactoring Type"]
    window_count = dataframe["Window Count"].values
    window_size = dataframe["Window Size Total"].values
    dataframe = dataframe.drop(["Refactoring Type", "Commit Count", "Window Size Total"], axis=1)

    #compute the probabilities
    dataframe_probability = compute_probability(dataframe, window_count, "window_" + table)

    #drop irrelevant rows, to simplify the plot
    co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns = filter_probability(dataframe_probability, probability_threshold, labels, "window_" + table)

    #plot the matrix
    #create a subplot, in order to name the columns and rows
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=160)
    im, cbar = heatmap(co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel="Co-occurence [P/ Commit]")
    plt.title("Co-occurrence of refactoring types in the same commit time window of &s (min[row | col] > %s)" % (table, probability_threshold))
    plt.savefig("results/Refactorings_co-occurrence_window_likelihood_%s_%s.png" % (probability_threshold, str(co_occurrence_matrix.shape)))
    print("Saved figure: Co-occurrence of refactoring types in the same commit time window of &s (min[row | col] > %s)" % (table, probability_threshold))

#all refactoring types
refactorings = ["Change Attribute Type",
                "Change Package",
                "Change Parameter Type",
                "Change Return Type",
                "Change Variable Type",
                "Extract And Move Method",
                "Extract Attribute",
                "Extract Class",
                "Extract Interface",
                "Extract Method",
                "Extract Subclass",
                "Extract Superclass",
                "Extract Variable",
                "Inline Method",
                "Inline Variable",
                "Merge Parameter",
                "Merge Variable",
                "Move And Inline Method",
                "Move And Rename Attribute",
                "Move And Rename Class",
                "Move And Rename Method",
                "Move Attribute",
                "Move Class",
                "Move Method",
                "Move Source Folder",
                "Parameterize Variable",
                "Pull Up Attribute",
                "Pull Up Method",
                "Push Down Attribute",
                "Push Down Method",
                "Rename Attribute",
                "Rename Class",
                "Rename Method",
                "Rename Parameter",
                "Rename Variable",
                "Replace Attribute",
                "Replace Variable With Attribute",
                "Split Parameter",
                "Split Variable"]
log_init()
log('Begin Statistics')
start_time = time.time()

#Co-occurrence of refactoring types on the same commit
co_occurence_commit(refactorings)
co_occurence_commit(refactorings, 0.1)
co_occurence_commit(refactorings, 0.2)
co_occurence_commit(refactorings, 0.3)
co_occurence_commit(refactorings, 0.5)

#Co-occurrence of refactoring types in the same commit window

log('Processing statistics took %s seconds.' % (time.time() - start_time))
log_close()

exit()
