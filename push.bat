@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo  Initializing Git ^& Releasing FastContacts (0.1.0)
echo ===================================================

git init -b main
git add .
git commit -m "feat: Initial release 0.1.0 of FastContacts"

echo Creating GitHub Repository andrestubbe/FastContacts...
gh repo create andrestubbe/FastContacts --public --source=. --push

echo Creating Git Tag 0.1.0...
git tag 0.1.0
git push origin 0.1.0

echo Creating GitHub Release 0.1.0...
gh release create 0.1.0 --title "FastContacts 0.1.0" --notes "Initial release of FastContacts: High-speed CardDAV and vCard (.vcf) contacts registry for Java 17+."

echo ===================================================
echo  FastContacts 0.1.0 Released Successfully!
echo ===================================================
pause
