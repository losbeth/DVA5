; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!


#include "java.iss"

[Setup]
AppName=DVA
AppVerName=DVA 
AppPublisher=Jonathan Boles
CloseApplications=yes
CloseApplicationsFilter=*.exe;*.dll;*.chm;*.jar
DefaultDirName={pf}\DVA 5
DefaultGroupName=DVA 5
SourceDir=.
OutputDir=..\build\Debug
OutputBaseFilename=DVA5Setup
Compression=none
;lzma2/max
SolidCompression=yes
UninstallDisplayIcon={app}\DVA.exe
ArchitecturesInstallIn64BitMode=x64

WindowVisible=no
;BackColor=$884422
WindowShowCaption=no


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\build\Debug\win\i386\DVA.exe"; DestDir: "{app}"; Flags: replacesameversion; Check: not IsWin64
Source: "..\build\Debug\win\i386\ttfetch.exe"; DestDir: "{app}"; Flags: replacesameversion; Check: not IsWin64
Source: "..\build\Debug\win\amd64\DVA.exe"; DestDir: "{app}"; Flags: replacesameversion; Check: IsWin64
Source: "..\build\Debug\win\amd64\ttfetch.exe"; DestDir: "{app}"; Flags: replacesameversion; Check: IsWin64
Source: "..\build\Debug\win\i386\DVA.scr"; DestDir: "{sys}"; Flags: replacesameversion; Check: not IsWin64
Source: "..\build\Debug\win\amd64\DVA.scr"; DestDir: "{sys}"; Flags: replacesameversion; Check: IsWin64
Source: "..\logging.properties"; DestDir: "{app}"
Source: "..\jars\*.jar"; DestDir: "{app}"
Source: "..\jars\win\*.jar"; DestDir: "{app}"
Source: "..\build\Debug\*.jar"; DestDir: "{app}"
Source: "..\build\Debug\*.txt"; DestDir: "{app}"
Source: "..\build\Debug\src.zip"; DestDir: "{app}"
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Dirs]
Name: "{commonappdata}\DVA"; Permissions: Users-modify

[Icons]
Name: "{group}\DVA 5"; Filename: "{app}\DVA.exe"; AppUserModelID: "jb.DVA"
Name: "{group}\{cm:UninstallProgram,DVA}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\DVA 5"; Filename: "{app}\DVA.exe"; Tasks: desktopicon; AppUserModelID: "jb.DVA"

[Registry]
Root: HKLM; Subkey: "Software\JavaSoft"; ValueType: string; ValueName: "SPONSORS"; ValueData: "DISABLE"
Root: HKLM; Subkey: "Software\DVA"; ValueType: string; ValueName: "working.directory"; ValueData: "{app}"; Flags: deletevalue
Root: HKLM; Subkey: "Software\Wow6432Node\JavaSoft"; ValueType: string; ValueName: "SPONSORS"; ValueData: "DISABLE"

[Run]
Filename: "{app}\DVA.exe"; Description: "{cm:LaunchProgram,DVA}"; Flags: nowait postinstall
Filename: "{app}\DVA.exe"; Parameters: "/x"; StatusMsg: "Updating sound libraries"

[Code]

