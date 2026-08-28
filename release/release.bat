@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0\.."

echo ===================================================
echo  Releasing FastContacts 0.1.0 to GitHub
echo ===================================================

git init -b main
git add .
git commit -m "feat: Initial release 0.1.0 of FastContacts"
gh repo create andrestubbe/FastContacts --public --source=. --push
git tag 0.1.0
git push origin 0.1.0
gh release create 0.1.0 --title "FastContacts 0.1.0" --notes-file "release/youtube_description.txt"

echo Release 0.1.0 published!
