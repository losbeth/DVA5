; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
AppName=DVA
AppVerName=DVA
AppVersion={#version}
AppPublisher=Jonathan Boles
CloseApplications=yes
CloseApplicationsFilter=*.exe;*.dll;*.chm;*.jar
DefaultDirName={pf}\DVA 5
DefaultGroupName=DVA 5
SourceDir=.
OutputDir=..\build\Output
OutputBaseFilename=DVA5Setup
Compression={#innosetupcompression}
SolidCompression=yes
UninstallDisplayIcon={app}\DVA.exe
ArchitecturesInstallIn64BitMode=x64

WindowVisible=no
;BackColor=$884422
WindowShowCaption=no

[InstallDelete]
Type: filesandordirs; Name: "{app}\jre"

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\build\Output\win\i386\DVA.exe"; DestDir: "{app}"; Flags: replacesameversion; Check: not IsWin64
Source: "..\build\Output\win\i386\DVA.scr"; DestDir: "{sys}"; Flags: replacesameversion; Check: not IsWin64
Source: "..\build\Tools\jre\win32\*"; DestDir: "{app}\jre"; Flags: recursesubdirs replacesameversion; Check: not IsWin64
Source: "..\ffmpeg\win\i686\*"; DestDir: "{app}"; Flags: replacesameversion; Check: not IsWin64

Source: "..\build\Output\win\amd64\DVA.exe"; DestDir: "{app}"; Flags: replacesameversion; Check: IsWin64
Source: "..\build\Output\win\amd64\DVA.scr"; DestDir: "{sys}"; Flags: replacesameversion; Check: IsWin64
Source: "..\build\Tools\jre\win64\*"; DestDir: "{app}\jre"; Flags: recursesubdirs replacesameversion; Check: IsWin64
Source: "..\ffmpeg\win\amd64\*"; DestDir: "{app}"; Flags: replacesameversion; Check: IsWin64

Source: "..\jars\*.jar"; DestDir: "{app}"
Source: "..\build\Output\*.jar"; DestDir: "{app}"
Source: "..\build\Output\*.txt"; DestDir: "{app}"
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\DVA 5"; Filename: "{app}\DVA.exe"; AppUserModelID: "jb.DVA"
Name: "{group}\{cm:UninstallProgram,DVA}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\DVA 5"; Filename: "{app}\DVA.exe"; Tasks: desktopicon; AppUserModelID: "jb.DVA"

[Registry]
Root: HKLM; Subkey: "Software\DVA"; ValueType: string; ValueName: "working.directory"; ValueData: "{app}"; Flags: deletevalue

[Run]
Filename: "{app}\DVA.exe"; Description: "{cm:LaunchProgram,DVA}"; Flags: nowait postinstall
Filename: "{app}\DVA.exe"; Parameters: "/x"; StatusMsg: "Updating sound libraries"


