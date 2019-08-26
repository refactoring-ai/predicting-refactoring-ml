import os

base_dir = "/Users/mauricioaniche/Desktop/drive2/rq1-raw-logs/"


def find_files(base_dir, pattern):
    files = []
    for r, d, f in os.walk(base_dir):
        for file in f:
            if pattern in file:
                files.append(os.path.join(r, file))

    return files


list_of_files_for_processing = find_files(base_dir, "variable-level")
collection_of_all = []
best_params = False

for file_name in list_of_files_for_processing:
    # print("opening file " + file_name)
    f = open(file_name, "r")

    parts_of_file_name = file_name.replace(base_dir, "").split("-")
    dataset = parts_of_file_name[3].replace(".txt", "")

    for line in f:
        if line.startswith("**** Refactoring "):
            current_refactoring = line[18:].strip()
            # print(current_refactoring)

        if line.startswith("Model "):
            current_model = line[7:].strip()
            # print(current_model)

        if line.startswith("Best parameters:"):
            best_params = True
            continue

        if best_params:
            if line.startswith("{"):
                continue

            elif not line.startswith("}"):
                p = line.strip().replace("\"", "").replace(",", "")
                collection_of_all.append(p)

            else:
                in_str = str(collection_of_all)
                in_str = in_str.replace(",", ";")
                print(dataset + "," + current_model + "," + current_refactoring + "," + in_str)

                collection_of_all = []
                best_params = False

    f.close()
