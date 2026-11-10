# Analysis Scripts

Within this directory are the analysis scripts of [Team 8858 "Beast From the East"](https://www.thebluealliance.com/team/8858).

- Under `tba_analysis/` is a library of functions that can simplify the process of scripting data-collection from [The Blue Alliance's API](https://www.thebluealliance.com/apidocs).
- Other folders below this directory are examples of some of the applications we've developed to collect data and analyze performance.

## Prerequisites

- Python 3.x
- Virtual Environment (recommended)
- The Blue Alliance API Key

## Installation

1. Clone the repository:

```bash
git clone <repository-url>
# navigate to this directory under the cloned repository
cd 8858_FRC_code/analysis_scripts
```

1. Set up the `tba_analysis` libraries which contain functions used to parse [The Blue Alliance's API](https://www.thebluealliance.com/apidocs):

```bash
# uses setup.py to set up the libraries described in it
pip install -e .
# remember this^ dot!
```

1. Set up the API Key
    - API Key can be generated from The Blue Alliance's [account page](https://www.thebluealliance.com/account) under "Read API Keys"
    - Each user only needs one key
    - automated method (recommended)
        - Running any of the apps will prompt you on providing the API Key if it's detected that this key has not been provided previously
        - API Key will be stored inside of `./tba_analysis/api_key.py` (relative to this directory)
    - manual method (not recommended)
        - you can make the `api_key.py` file yourself under the `tba_analysis` folder
        - This file should only contain an assignment of your API Key within quotes to a variable named "`API_KEY`"

After completing these steps, you'll be ready to either run one of the existing applications or start developing your own!

## Usage

Recommended method to import the libraries:

```python
from tba_analysis.analysis_func import *
```

- This import will automatically import the `requests`, `os`, `sys` and `Path` modules.
- This will also handle setup of the API Key (`API_KEY`) and provide a header (`headers`) required to make data requests to the API.
- The variable `BASE_URL` will point to the base address of the API's database.
- Terminal print verbosity is controlled by `set_verbosity(level)`: `0` = quiet (errors/warnings only), `1` = normal (default, key results), `2` = verbose (raw data/debug prints). CLI scripts expose this via `-v {0,1,2}` (with `-d`/`-debug_mode` kept as a deprecated alias for `-v 2`).

## MCP server for VS Code Copilot

This repository includes a local, read-only MCP server for common FRC information queries. It provides team information, team events, event details, event teams, matches, rankings, and estimated match rank changes without requiring a custom Python script.

Set the API key in the environment before starting the server. The existing ignored `tba_analysis/api_key.py` file is also supported for compatibility, but environment configuration is preferred:

```powershell
$env:TBA_API_KEY = "your-read-api-key"
pip install -e .
python -m tba_mcp.server
```

Add the following to `.vscode/mcp.json` (keep the key out of committed files):

```json
{
    "servers": {
        "tba": {
            "type": "stdio",
            "command": "${workspaceFolder}\\.venv\\Scripts\\python.exe",
            "args": ["-m", "tba_mcp.server"],
            "cwd": "${workspaceFolder}\\analysis_scripts",
            "env": {
                "TBA_API_KEY": "${env:TBA_API_KEY}"
            }
        }
    }
}
```

Example prompts include “What events did team 8858 attend in 2026?”, “List the matches team 8858 played at `2026code`”, and “What are the current rankings at `2026code`?”.
