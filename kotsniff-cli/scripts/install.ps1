param (
    [Parameter(Mandatory=$true)]
    [string]$InstallDistDir
)

$TargetDir = Join-Path $env:USERPROFILE ".kotsniff"
$BinDir = Join-Path $TargetDir "bin"

Write-Host "Installing KotSniff CLI to $TargetDir..."

# Remove old installation if exists
if (Test-Path $TargetDir) {
    Remove-Item -Path $TargetDir -Recurse -Force
}

# Copy the compiled application
Copy-Item -Path $InstallDistDir -Destination $TargetDir -Recurse -Force

Write-Host "Successfully copied KotSniff CLI to $TargetDir"

# Add to PATH if not already there
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if (-not $UserPath.Contains($BinDir)) {
    Write-Host "Adding $BinDir to your User PATH environment variable..."
    $NewPath = $UserPath + ";" + $BinDir
    [Environment]::SetEnvironmentVariable("Path", $NewPath, "User")
    Write-Host "KotSniff CLI installed successfully!"
    Write-Host "Please restart your terminal to use the 'kotsniff' command."
} else {
    Write-Host "KotSniff CLI installed successfully! It is already in your PATH."
}
