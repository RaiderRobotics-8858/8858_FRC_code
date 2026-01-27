# PowerShell script to call gen_docs.py with manually defined input files

# Manually defined list of input files
$mdFiles = @(
    "Summary.md"
)

# Build the argument list
$vars = @("-f") + $mdFiles + @("-o", "E_robot.pdf")

# Call gen_docs.py
& python gen_docs.py $vars