import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from db.DBConnector import execute_query
from statistics.plot_utils import heatmap
from utils.log import log_init, log_close, log
import time

#Computes the occurrence probability of refactorings occuring on the same commit and creates a heatmap for it
#param refactorings: all relevant refactoring types
#param probability_threshold: the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
def co_occurence_commit(refactorings, probability_threshold = 0.20):
    dataframe = pd.DataFrame()
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

    dataframe = pd.read_csv("results/Refactorings_commit_statistics.csv")

    # extract the labels for the plot
    labels = dataframe["Refactoring Type"]
    commit_counts = dataframe["Commit Count"]
    dataframe = dataframe.drop(["Refactoring Type", "Commit Count"], axis=1)
    #compute the probabilty of a refatoring co-occuring on the same commit
    dataframe_probality = dataframe.div(commit_counts.values)

    #set the diagonal to zero, in order to clean up the matrix
    np.testing.assert_array_equal(np.diag(dataframe_probality), 1.0, err_msg="Not all elements on the diagonal are zero.")
    np.fill_diagonal(dataframe_probality.values, 0)
    dataframe_probality.to_csv("results/Refactorings_commit_statistics_prepared.csv", index=False)

    #drop irrelevant rows, to simplify the plot
    filtered_rows_matrix, filtered_labels_rows, filtered_labels_columns, drop_columns = [], [], [], []
    #filter all columns
    for name, values in dataframe_probality.iteritems():
        if values.max() <= probability_threshold:
            drop_columns.append(name)
        else:
            filtered_labels_columns.append(name)
    dataframe_probality_filtered = dataframe_probality.drop(drop_columns, axis=1)
    #filter all rows
    for index, row in dataframe_probality_filtered.iterrows():
        if row.max() > probability_threshold:
            filtered_rows_matrix.append(row)
            filtered_labels_rows.append(labels[index])
    co_occurrence_matrix = pd.DataFrame(filtered_rows_matrix, columns=dataframe_probality_filtered.columns)
    print("Co-occurrence matrix shape: " + str(co_occurrence_matrix.shape))

    #plot the matrix
    #create a subplot, in order to name the columns and rows
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=160)
    im, cbar = heatmap(co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel="Co-occurence [P/ Commit]")
    plt.title("Co-occurence of refactoring types on the same commit (min[row | col] > %s)" % probability_threshold)
    plt.savefig("results/Refactorings_commit_statistics_%s_%s.png" % (probability_threshold, str(co_occurrence_matrix.shape)))

#Computes the occurrence probability of refactorings occurring on the within a time window and creates a heatmap for it
#param refactorings: time range in hours around a specific commit, to consider
#param refactorings: all relevant refactoring types
#param probability_threshold: the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
def co_occurence_window(refactorings, time_range, probability_threshold = 0.20):
    dataframe = pd.DataFrame()
    for refactoring_name in refactorings:
        query = "SELECT "
        query += ", ".join(["SUM(IF(`%s count` > 0, 1, 0)) AS `%s`" % (refactoring_type, refactoring_type) for refactoring_type in refactorings])
        query += ", COUNT(commitMetaData_id) AS `Commit Windows Count` "
        query += ("FROM refactoringspercommit WHERE `%s count` > 0" % refactoring_name)
        refactoringspercommit = execute_query(query)
        refactoringspercommit["Refactoring Type"] = refactoring_name
        dataframe = pd.concat([dataframe, refactoringspercommit])
    #store the dataframe to have the raw data
    dataframe.to_csv("results/Refactorings_commit_statistics.csv", index=False)

    dataframe = pd.read_csv("results/Refactorings_commit_statistics.csv")

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

co_occurence_commit(refactorings)

log('Processing statistics took %s seconds.' % (time.time() - start_time))
log_close()

exit()
