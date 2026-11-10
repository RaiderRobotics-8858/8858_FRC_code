"""
# [The Blue Alliance](https://www.thebluealliance.com) API Data-Fetching Functions
By [Team 8858 "Beast From the East"](https://www.thebluealliance.com/team/8858)

## Description

This Library was developed to assist in more customizable collection of data from The [Blue Alliance's API](https://www.thebluealliance.com/apidocs),
allowing teams to develop more unique methods of analyzing their performance.

* This library contains common functions that can be used to get data from The Blue Alliance's API
* The functions in this library can also be used to get examples of how to interface with The Blue Alliance's API

## Quick Start Guide

To install this library, follow these steps:

1. Copy the contents of the `tba_analysis` directory into the place you are developing in.
2. Create a `setup.py` script at the folder as `tba_analysis` that does the following:
    ```python
    from setuptools import setup, find_packages
    setup(
        name='tba_analysis',
        version='0.1',
        packages=find_packages(),
        install_requires=[]
    )
    ```
3. From the same directy as where `setup.py` and `tba_analysis` are stored, run the following:
    ```
    pip install -e .
    ```
4. From now on, when making new scripts, just add the following lines to get all functionality of this library:
    ```python
    from tba_analysis.analysis_func import *
    ```
5. Navigate to [The Blue Alliance's API account](https://www.thebluealliance.com/account) page and create an API key.
6. The first time you run a script with this library imported, `api_key.py` will be generated and you will be prompted to enter the API key from step 4.

    * Once the API key is entered once, you will not need to enter it again
    * `api_key.py` is an unmanaged file in the repository and should not be shared between users

7. TBA responses are cached under `tba_analysis/.cache` so repeated analysis can reuse the latest stored responses without refetching everything.

    * Use the shared fetch helper to force a refresh when you need fresh data
"""