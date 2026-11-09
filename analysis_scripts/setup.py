from setuptools import setup, find_packages
from pathlib import Path

setup(
    name='tba_analysis',
    version='0.1',
    packages=find_packages(),
    description="Functions to simplify parseing The Blue Alliance's API",
    long_description=(Path(__file__).resolve().parent / "tba_analysis/README.md").read_text(encoding="utf-8"),
    long_description_content_type='text/markdown',
    install_requires=[
        'requests>=2.31,<3',
        'colorama>=0.4.6,<1',
        'mcp[cli]>=1.9,<2',
    ]
)
