from time import strftime, gmtime


def now():
    return strftime("%Y-%m-%d %H:%M:%S", gmtime())