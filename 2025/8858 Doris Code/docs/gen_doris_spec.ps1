# PowerShell script to call gen_docs.py with manually defined input files

# Manually defined list of input files
$mdFiles = @(
    "DorisSummary.md",
    "ButtonMapping.md",
    "ElevatorSubsystem.md",
    "CoralIntake.md",
    "Limelight.md"
)

# Build the argument list
$vars = @("-f") + $mdFiles + @("-o", "Doris.pdf")

# Call gen_docs.py
& python gen_docs.py $vars