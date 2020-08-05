from enum import Enum

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from db.DBConnector import execute_query
from refactoring_statistics.plot_utils import heatmap
from utils.log import log_init, log_close, log
import time
from os import path


class Statistics(Enum):
    """
    1. "likelihood": total sum of windows in which a refactoring type occurs
            and divide it by the total window count in which the current refactoring type occurs
    2. "frequency": total sum of the occurrences in a window of a refactoring
            divided by the total window count in which the current refactoring type occurs
    """
    likelihood = 1
    frequency = 2


def compute_probability(dataframe, divisor, file_addition: str, statistic: Statistics = Statistics.likelihood):
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
    file_path = f"results/Refactorings_{file_addition}.csv"
    if not path.exists(file_path):
        #compute the probabilty of a refatoring co-occuring on the same commit
        dataframe_probability = dataframe.div(divisor)

        if statistic == Statistics.likelihood:
            #set the diagonal to zero, in order to clean up the matrix
            #np.testing.assert_array_equal(np.diag(dataframe_probability), 1.0, err_msg="Not all elements on the diagonal are zero.")
            np.fill_diagonal(dataframe_probability.values, 0)

        dataframe_probability.to_csv(file_path, index=False)
        log(f"Co-occurrence {statistic} matrix with shape: {dataframe_probability.shape} was computed and stored.")
        return dataframe_probability
    else:
        return pd.read_csv(file_path)


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
    file_addition
        Add this to the output file in order to distinguish it, e.g.
    """
    file_path = f"results/Refactorings_{file_addition}_{threshold:.2f}.csv"
    filtered_rows_matrix, filtered_labels_rows, filtered_labels_columns, drop_columns = [], [], [], []
    #filter all columns
    for name, values in dataframe.iteritems():
        if values.max() <= threshold:
            drop_columns.append(name)
        else:
            filtered_labels_columns.append(name)
    dataframe_probabality_filtered = dataframe.drop(drop_columns, axis=1)
    #filter all rows
    index: int = 0
    # the index of iterrows, is not reliable, sometimes it always returns zero
    for i, row in dataframe_probabality_filtered.iterrows():
        if row.max() > threshold:
            filtered_rows_matrix.append(row)
            filtered_labels_rows.append(labels[index])
        index += 1
    co_occurrence_matrix = pd.DataFrame(filtered_rows_matrix, columns=dataframe_probabality_filtered.columns)
    co_occurrence_matrix.to_csv(file_path, index=False)
    log(f"Co-occurrence matrix with shape: {co_occurrence_matrix.shape} and threshold: {threshold:.2f} was computed and stored.")
    return co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns


def query_db(table_name: str, entity_name: str, refactorings, statistic = Statistics.likelihood):
    dataframe = pd.DataFrame()
    file_path = f"results/Refactorings_window_{table_name}_{statistic}_raw.csv"
    if not path.exists(file_path):

        for refactoring_name in refactorings:
            query = "SELECT "
            if statistic == Statistics.likelihood:
                query += ", ".join([f"SUM(IF(`{refactoring_type}` > 0, 1, 0)) AS `{refactoring_type}`" for refactoring_type in refactorings])
            elif statistic == Statistics.frequency:
                query += ", ".join([f"SUM(`{refactoring_type}`) AS `{refactoring_type}`" for refactoring_type in refactorings])

            query += f",COUNT(*) AS `{entity_name} Count Total` "
            query += f"FROM {table_name} WHERE `{refactoring_name}` > 0"
            refactoringspercommit = execute_query(query)
            refactoringspercommit["Refactoring Type"] = refactoring_name
            dataframe = pd.concat([dataframe, refactoringspercommit])
        #store the dataframe to have the raw data
        dataframe.to_csv(file_path, index=False)
        log(f"Got the raw data from Refactorings_window_{table_name} and stored it in: {file_path}.")
    else:
        dataframe = pd.read_csv(file_path)

    # extract the labels for the plot and remove un-plotted data
    labels = dataframe["Refactoring Type"].values
    count = dataframe[f"{entity_name} Count Total"].values
    dataframe = dataframe.drop(["Refactoring Type", f"{entity_name} Count Total"], axis=1)
    return dataframe, labels, count


def co_occurence_commit(refactorings, probability_threshold = 0.0, statistic = Statistics.likelihood):
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
    dataframe, labels, commit_count = query_db("refactoringspercommit", "Window", refactorings, statistic)

    #compute the probabilities
    dataframe_probability = compute_probability(dataframe, commit_count, "commit")

    #drop irrelevant rows, to simplify the plot
    co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns = filter_probability(dataframe_probability, probability_threshold, labels, "commit")

    #plot the matrix
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=160)
    im, cbar = heatmap(co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel=f"Co-occurrence [{statistic} / COMMIT]")

    plt.title(f"Co-occurrence of refactoring types on the same commit (min[row | col] > {probability_threshold:.2f})")
    plt.savefig(f"results/Refactorings_co-occurrence_commit_likelihood_{probability_threshold:.2f}_{co_occurrence_matrix.shape}.png")
    log(f"Saved figure: Co-occurrence of refactoring types on the same commit (min[row | col] > {probability_threshold:.2f})")


def co_occurence_window(refactorings, table_name: str, probability_threshold=0.0, statistic: Statistics = Statistics.likelihood):
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
    table_name
        Addition to the window table name, e.g. 6H
    probability_threshold
        the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
    statistic
        How to compute the likelihood to occur of the of the refactoring.
    """
    dataframe, labels, window_count = query_db(table_name, "Window", refactorings, statistic)

    #compute the probabilities
    dataframe_probability = compute_probability(dataframe, window_count, f"window_{table_name}_{statistic}", statistic)

    #drop irrelevant rows, to simplify the plot
    co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns = filter_probability(dataframe_probability, probability_threshold, labels, f"window_{table_name}_{statistic}")

    #plot the matrix
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=120)
    im, cbar = heatmap(co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel=f"Co-occurrence [{statistic} / WINDOW]")
    plt.title(f"Co-occurrence of refactoring types in the same commit time window of {table_name} (min[row | col] > {probability_threshold:.2f})")
    plt.savefig(f"results/Refactorings_co-occurrence_window_{probability_threshold:.2f}_{statistic}_{co_occurrence_matrix.shape}.png")
    log(f"Saved figure: Co-occurrence of refactoring types in the same commit time window of {table_name} (min[row | col] > {probability_threshold:.2f})")


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

log_init(header=False, config=False)
log('Begin Statistics')
start_time = time.time()

log(f"-- Refactorings on the same commit")
#Co-occurrence of refactoring types on the same commit
for threshold in np.arange(0.0, 0.6, 0.1):
    log(f"---- Refactorings on the same commit with likelihood")
    co_occurence_commit(refactorings, threshold, Statistics.likelihood)
    log(f"---- Refactorings on the same commit with frequency")
    co_occurence_commit(refactorings, threshold, Statistics.frequency)

log(f"\n-- Refactorings in a time window")
#Co-occurrence of refactoring types in the same commit window
for table in [6, 12, 24]:
    log(f"---- Refactorings in a window of {table} hours with likelihood")
    for threshold in np.arange(0.0, 0.51, 0.1):
        co_occurence_window(refactorings, str(table) + "H", threshold, Statistics.likelihood)

    log(f"---- Refactorings in a window of {table} hours with frequency")
    co_occurence_window(refactorings, f"RefactoringsWindow_{table}H", 0.0, Statistics.frequency)

log(f"Processing statistics took {time.time() - start_time:.2f} seconds.")
log_close()

exit()
