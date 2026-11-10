
"""
# Analysis Functions Library
"""

import json
import hashlib
import requests
import os
import sys
import datetime
import shutil
import time
from pathlib import Path
from colorama import Fore, Style, just_fix_windows_console

just_fix_windows_console()

CACHE_DIR = Path(__file__).resolve().parent / ".cache"
_memory_cache = {}

# Handle the API Key for The Blue Alliance
def _create_api_key_file():
    """Prompt for and save the API key for interactive analysis scripts."""
    api_key_path = Path(__file__).resolve().parent / "api_key.py"
    warning_msg("An API key was not found. Generate one at https://www.thebluealliance.com/account")
    api_key = input("Enter your TBA API key (leave blank to add it later):").strip()
    if not api_key:
        api_key = "your_api_key_here"
        warning_msg(f"API key left blank; add it later to {api_key_path}")

    api_key_path.write_text(
        '"""Your Key for [The Blue Alliance\'s API](https://www.thebluealliance.com/apidocs)"""\n\n'
        f'API_KEY = "{api_key}"\n',
        encoding="utf-8",
    )
    return "" if api_key == "your_api_key_here" else api_key


def _load_api_key():
    """Load credentials, retaining interactive setup for legacy scripts."""
    environment_key = os.environ.get("TBA_API_KEY")
    if environment_key:
        return environment_key

    try:
        from tba_analysis.api_key import API_KEY as configured_key
    except ImportError:
        if os.environ.get("TBA_NO_INTERACTIVE_API_KEY") == "1":
            return ""
        return _create_api_key_file()

    if configured_key == "your_api_key_here":
        if os.environ.get("TBA_NO_INTERACTIVE_API_KEY") == "1":
            return ""
        return _create_api_key_file()
    return configured_key


API_KEY = _load_api_key()
headers = {"X-TBA-Auth-Key": API_KEY} if API_KEY else {}

# Base URL for TBA API
BASE_URL = "https://www.thebluealliance.com/api/v3"

# Verbosity levels: 0 = quiet (errors/warnings only), 1 = normal (key results, default), 2 = verbose (raw data/debug)
VERBOSITY = 1


def set_verbosity(level):
    """
    Sets the module-wide verbosity level used by `log_msg()` for terminal prints.
    0 = quiet, 1 = normal (default), 2 = verbose/debug
    """
    global VERBOSITY
    VERBOSITY = level


def log_msg(message, level=1):
    """
    Prints an informational message only when the current VERBOSITY is >= `level`.
    """
    if VERBOSITY >= level:
        info_msg(message)


def _print_status(label, message, foreground):
    print(f"[{foreground}{label}{Style.RESET_ALL}]: {message}")


def info_msg(message):
    """Print an informational message with a green status label."""
    _print_status("INFO", message, Fore.GREEN)


def warning_msg(message):
    """Print a warning message with a yellow status label."""
    _print_status("WARNING", message, Fore.YELLOW)


def error_msg(message):
    """Print an error message with a red status label."""
    _print_status("ERROR", message, Fore.RED)


def _cache_path(url):
    cache_key = hashlib.sha256(url.encode("utf-8")).hexdigest()
    return CACHE_DIR / f"{cache_key}.json"


def _structured_event_cache_path(url):
    event_prefix = f"{BASE_URL}/event/"
    if not url.startswith(event_prefix):
        return None

    event_suffix = url[len(event_prefix):]
    if not event_suffix:
        return None

    event_parts = event_suffix.split("/")
    event_key = event_parts[0]
    if not event_key:
        return None

    event_dir = CACHE_DIR / "events" / event_key
    if len(event_parts) == 1:
        return event_dir / "event.json"

    endpoint_name = event_parts[1]
    endpoint_map = {
        "matches": "matches.json",
        "teams": "teams.json",
        "rankings": "rankings.json",
        "awards": "awards.json",
        "district_points": "district_points.json",
        "alliances": "alliances.json",
        "insights": "insights.json",
    }

    file_name = endpoint_map.get(endpoint_name)
    if file_name is None:
        file_name = f"{'_'.join(event_parts[1:])}.json"

    return event_dir / file_name


def _load_json_file(cache_file):
    try:
        with cache_file.open("r", encoding="utf-8") as cache_handle:
            cached_payload = json.load(cache_handle)
        if isinstance(cached_payload, dict) and "data" in cached_payload:
            return cached_payload.get("data")
        return cached_payload
    except (OSError, json.JSONDecodeError):
        return None


def _load_cached_json(url):
    if url in _memory_cache:
        return _memory_cache[url]

    structured_cache_file = _structured_event_cache_path(url)
    if structured_cache_file is not None and structured_cache_file.exists():
        cached_data = _load_json_file(structured_cache_file)
        if cached_data is not None:
            _memory_cache[url] = cached_data
            return cached_data

    cache_file = _cache_path(url)
    if not cache_file.exists():
        return None

    cached_data = _load_json_file(cache_file)
    if cached_data is not None:
        _memory_cache[url] = cached_data
    return cached_data


def _store_structured_event_cache(url, data):
    structured_cache_file = _structured_event_cache_path(url)
    if structured_cache_file is None:
        return

    structured_cache_file.parent.mkdir(parents=True, exist_ok=True)
    with structured_cache_file.open("w", encoding="utf-8") as cache_handle:
        json.dump(data, cache_handle, indent=2)


def _store_cached_json(url, data, status_code=200):
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    _memory_cache[url] = data

    cache_payload = {
        "url": url,
        "cached_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "status_code": status_code,
        "data": data,
    }

    cache_file = _cache_path(url)
    with cache_file.open("w", encoding="utf-8") as cache_handle:
        json.dump(cache_payload, cache_handle, indent=2)

    _store_structured_event_cache(url, data)


def fetch_tba_json(url, force_refresh=False):
    """
    Fetch JSON from TBA with a cache-first strategy.
    """
    cached_data = None if force_refresh else _load_cached_json(url)
    if cached_data is not None:
        return cached_data

    while True:
        try:
            response = requests.get(url, headers=headers, timeout=10)
            break
        except requests.exceptions.ConnectionError as request_error:
            # DNS/connection failures are usually transient (no internet, TBA down) so keep retrying
            warning_msg(f"DNS failed, trying again in 5 seconds... ({request_error})")
            time.sleep(5)
        except requests.exceptions.RequestException as request_error:
            # non-connection request errors (e.g. timeouts): fall back to cache if we have one
            if cached_data is not None:
                return cached_data
            error_msg(f"Failed to reach TBA API for {url}: {request_error}")
            raise RuntimeError(f"Unable to reach TBA API for {url}: {request_error}") from request_error

    if response.status_code == 200:
        data = response.json()
        _store_cached_json(url, data, response.status_code)
        return data

    if cached_data is not None:
        return cached_data

    error_msg(f"Failed to fetch data: {response.status_code} - {response.reason}")
    # raise instead of exit() so a single failed request doesn't kill a long-running server process
    raise RuntimeError(f"Unable to fetch cached or live data for {url} (HTTP {response.status_code} - {response.reason})")


def refresh_tba_json(url):
    """
    Force a fresh fetch for a TBA endpoint and update the cache.
    """
    return fetch_tba_json(url, force_refresh=True)


def clear_tba_cache(include_exports=True):
    """
    Clear the local TBA cache and reset in-memory cached responses.
    """
    global _memory_cache
    _memory_cache = {}

    if not CACHE_DIR.exists():
        return

    if include_exports:
        shutil.rmtree(CACHE_DIR)
        return

    for cache_item in CACHE_DIR.iterdir():
        if cache_item.is_file():
            cache_item.unlink()
        elif cache_item.is_dir() and cache_item.name != "exports":
            shutil.rmtree(cache_item)

def clear():
    """
    function to clear the screen
    """
    if sys.platform == 'win32':
        _ = os.system('cls')
    else:
        _ = os.system('clear')

# team/event/match/season-specific functions live in team_func.py, event_func.py,
# match_func.py, and season_func.py; re-exported below for backward compatibility.
from tba_analysis.team_func import *
from tba_analysis.event_func import *
from tba_analysis.match_func import *
from tba_analysis.season_func import *
