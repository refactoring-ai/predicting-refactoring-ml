# creating enumerations using class
import enum

class FileType(enum.Enum):
    only_production = 0
    only_test = 1
    test_and_production = 2