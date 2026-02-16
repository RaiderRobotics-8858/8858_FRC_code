
# Input files for pandoc generation, in the desired order

$inputFiles = @(
    "pandocs_cover_and_config.md", # pandocs_cover_and_config.md should be first since it defines parameters for the entire document as well as the cover page.
    "lists_after_toc.md", # lists_after_toc.md should be second since it places the lists of figures and tables after the table of contents, which is a common convention.
    "Summary.md", # High-Level summary of the robot and button mappings for controller
    "interfaces.md", # CAN IDs, Digital I/O ports, Analog ports, etc.
    "subsystem_swerve.md", # Drivetrain
    "subsystem_intake.md", # Intake
    "subsystem_launcher.md", # Launcher
    "subsystem_vision.md", # Vision
    "subsystem_climber.md", # Climber
    "subsystem_led.md" # LED
)

$outputFile = "Echidna_Pandoc_Spec.pdf"
$tempDir = ".pandoc_tmp"

# Want table captions to render as above the tables in the markdown,
# but pandocs needs them to be below the tables to render correctly in the PDF.
# This function moves them below the tables before pandoc processes the files.
function Move-TableCaptionsBelow {
    param (
        [string[]]$Lines
    )

    $output = New-Object System.Collections.Generic.List[string]
    $index = 0
    while ($index -lt $Lines.Count) {
        $line = $Lines[$index]
        if ($line -match '^Table:\s*') {
            $caption = $line
            $nextIndex = $index + 1
            while ($nextIndex -lt $Lines.Count -and $Lines[$nextIndex].Trim() -eq "") {
                $nextIndex++
            }
            if ($nextIndex -lt $Lines.Count -and $Lines[$nextIndex].TrimStart().StartsWith("|")) {
                $index = $nextIndex
                while ($index -lt $Lines.Count -and $Lines[$index].TrimStart().StartsWith("|")) {
                    $output.Add($Lines[$index])
                    $index++
                }
                if ($output.Count -gt 0 -and $output[$output.Count - 1].Trim() -ne "") {
                    $output.Add("")
                }
                $output.Add($caption)
                continue
            }
        }

    $output.Add($line)
    $index++
    }

    return $output
}

# Pandoc doesn't support page breaks in markdown, so we use a custom marker and replace it with the appropriate LaTeX command before processing.
function Replace-PagebreakMarkers {
    param (
        [string[]]$Lines
    )

    $updated = New-Object System.Collections.Generic.List[string]
    foreach ($line in $Lines) {
        if ($line.Trim() -eq "<!-- pagebreak -->") {
            if ($updated.Count -gt 0 -and $updated[$updated.Count - 1].Trim() -ne "") {
                $updated.Add("")
            }
            $updated.Add("~~~{=latex}")
            $updated.Add("\newpage")
            $updated.Add("~~~")
            $updated.Add("")
            continue
        }
        $updated.Add($line)
    }

    return $updated
}

foreach ($inputFile in $inputFiles) {
    if (-not (Test-Path $inputFile)) {
        Write-Error "Input file not found: $inputFile"
        exit 1
    }
}

# Ensure required tools are available
if (-not (Get-Command pandoc -ErrorAction SilentlyContinue)) {
    Write-Error "Pandoc is not available on PATH. Install Pandoc and restart your terminal."
    exit 1
}

if (-not (Get-Command xelatex -ErrorAction SilentlyContinue)) {
    Write-Error "XeLaTeX is not available on PATH. Install a TeX distribution (MiKTeX or TeX Live) and restart your terminal."
    exit 1
}

if (-not (Get-Command inkscape -ErrorAction SilentlyContinue)) {
    Write-Error "Inkscape is not available on PATH. Install Inkscape and restart your terminal (winget install Inkscape.Inkscape)."
    exit 1
}

# Ensure required TeX packages are available for XeLaTeX
$unicodeMathPath = & kpsewhich unicode-math.sty 2>$null
if (-not $unicodeMathPath) {
    Write-Error "Missing TeX package 'unicode-math'. Install it via MiKTeX Console or run 'mpm --install=unicode-math', then re-run this script."
    exit 1
}

if (Test-Path $tempDir) {
    Remove-Item -Recurse -Force $tempDir
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

$fontsSource = Join-Path $PSScriptRoot "fonts"
$fontsDest = Join-Path $tempDir "fonts"
if (Test-Path $fontsSource) {
    Copy-Item -Path $fontsSource -Destination $fontsDest -Recurse -Force
}

$tempFiles = @()
foreach ($inputFile in $inputFiles) {
    $tempFile = Join-Path $tempDir $inputFile
    $lines = Get-Content -Path $inputFile
    $processedLines = Move-TableCaptionsBelow -Lines $lines
    $processedLines = Replace-PagebreakMarkers -Lines $processedLines
    $processedLines | Set-Content -Path $tempFile -Encoding UTF8
    $tempFiles += $tempFile
}

Write-Host "Generating PDF from:`n$($inputFiles -join "`n")"
& pandoc @tempFiles -o $outputFile --pdf-engine=xelatex --resource-path=.
$pandocExitCode = $LASTEXITCODE

if ($pandocExitCode -ne 0) {
    Write-Error "Pandoc failed with exit code $pandocExitCode"
    exit $pandocExitCode
}

Write-Host "Wrote $outputFile"
