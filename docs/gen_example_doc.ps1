# PowerShell script to call gen_docs.py with manually defined input files

# Manually defined list of input files
$mdFiles = @(
    "subsystem_template.md",
    "example_subsystem.md"
)

# Build the argument list
$args = @("-f") + $mdFiles + @("-o", "ExampleDoc.pdf")

# Call gen_docs.py
& python gen_docs.py $args