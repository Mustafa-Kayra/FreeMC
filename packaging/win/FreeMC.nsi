; FreeMC Windows kurulumu (NSIS). Gomulu JRE ile gelir, ek kurulum istemez.
Unicode true
SetCompressor /SOLID lzma

!define APPNAME "FreeMC"
!define VERSION "1.2.0"
!define PUBLISHER "FreeMC"
!define STAGE "payload"

RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\FreeMC"
InstallDirRegKey HKCU "Software\FreeMC" "InstallDir"

Name "${APPNAME} ${VERSION}"
OutFile "../../dist/FreeMC-Setup-${VERSION}.exe"
Icon "freemc.ico"
UninstallIcon "freemc.ico"

VIProductVersion "1.2.0.0"
VIAddVersionKey "ProductName" "${APPNAME}"
VIAddVersionKey "FileDescription" "${APPNAME} kurulumu"
VIAddVersionKey "FileVersion" "${VERSION}"
VIAddVersionKey "ProductVersion" "${VERSION}"
VIAddVersionKey "LegalCopyright" "${PUBLISHER}"
VIAddVersionKey "CompanyName" "${PUBLISHER}"

!include "MUI2.nsh"
!define MUI_ABORTWARNING
!define MUI_ICON "freemc.ico"
!define MUI_UNICON "freemc.ico"

!define MUI_WELCOMEPAGE_TITLE "FreeMC ${VERSION} kurulumu"
!define MUI_WELCOMEPAGE_TEXT "FreeMC, Minecraft'i cevrimdisi modda kuran ve baslatan bir launcher.$\r$\n$\r$\nJava ayrica kurmana gerek yok, kurulum paketi kendi Java 21 calisma ortamini getiriyor.$\r$\n$\r$\nDevam etmek icin Ileri'ye bas."

!define MUI_FINISHPAGE_RUN "$INSTDIR\FreeMC.exe"
!define MUI_FINISHPAGE_RUN_TEXT "FreeMC'yi simdi ac"
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\OKUBENI.txt"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Kullanim notlarini ac"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "Turkish"

Function .onInit
  SetShellVarContext current
  ; eski kurulum ayni klasordeyse sessizce guncelle
  ReadRegStr $0 HKCU "Software\FreeMC" "InstallDir"
  StrCmp $0 "" done
  StrCpy $INSTDIR $0
done:
FunctionEnd

Section "FreeMC" SecMain
  SetShellVarContext current
  SetOutPath "$INSTDIR"

  ; calisan surum dosyalari kilitliyorsa once kapat
  nsExec::Exec 'taskkill /f /im FreeMC.exe'
  nsExec::Exec 'taskkill /f /im javaw.exe /fi "WINDOWTITLE eq FreeMC*"'

  File "${STAGE}/FreeMC.exe"
  File "${STAGE}/freemc.jar"
  File "${STAGE}/OKUBENI.txt"
  SetOutPath "$INSTDIR\jre"
  File /r "${STAGE}/jre/*.*"
  SetOutPath "$INSTDIR"

  WriteRegStr HKCU "Software\FreeMC" "InstallDir" "$INSTDIR"
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  CreateDirectory "$SMPROGRAMS\FreeMC"
  CreateShortCut "$SMPROGRAMS\FreeMC\FreeMC.lnk" "$INSTDIR\FreeMC.exe" "" "$INSTDIR\FreeMC.exe" 0
  CreateShortCut "$SMPROGRAMS\FreeMC\FreeMC (konsol).lnk" "$SYSDIR\cmd.exe" '/c ""$INSTDIR\jre\bin\java.exe" -jar "$INSTDIR\freemc.jar""' "$INSTDIR\FreeMC.exe" 0
  CreateShortCut "$SMPROGRAMS\FreeMC\Kaldir.lnk" "$INSTDIR\Uninstall.exe"
  CreateShortCut "$DESKTOP\FreeMC.lnk" "$INSTDIR\FreeMC.exe" "" "$INSTDIR\FreeMC.exe" 0

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "DisplayName" "FreeMC"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "Publisher" "${PUBLISHER}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "DisplayIcon" "$INSTDIR\FreeMC.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "NoRepair" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC" "EstimatedSize" 160000
SectionEnd

Section "Uninstall"
  SetShellVarContext current
  nsExec::Exec 'taskkill /f /im FreeMC.exe'
  Delete "$DESKTOP\FreeMC.lnk"
  Delete "$SMPROGRAMS\FreeMC\FreeMC.lnk"
  Delete "$SMPROGRAMS\FreeMC\FreeMC (konsol).lnk"
  Delete "$SMPROGRAMS\FreeMC\Kaldir.lnk"
  RMDir "$SMPROGRAMS\FreeMC"

  Delete "$INSTDIR\FreeMC.exe"
  Delete "$INSTDIR\freemc.jar"
  Delete "$INSTDIR\OKUBENI.txt"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir /r "$INSTDIR\jre"
  RMDir "$INSTDIR"

  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\FreeMC"
  DeleteRegKey HKCU "Software\FreeMC"
  ; oyun dosyalari (%APPDATA%\.freemc) kullaniciya birakilir
SectionEnd
