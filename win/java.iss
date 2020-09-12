#include "idp.iss"
#include "version.iss"

[Run]
Filename: "{code:GetJavaDownloadPath}"; Parameters: "/s"; Description: "Install Java runtime"; StatusMsg: "Installing Java runtime"; Check: "NeedToInstallJava";

[Code]
function CheckJRE(MinVersion : String) : Boolean;
var
  JavaVer : String;
begin
    Result := false;
    if IsWin64 then
      begin
          RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\Java Runtime Environment', 'CurrentVersion', JavaVer);
      end
    else
      begin
          RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\JavaSoft\Java Runtime Environment', 'CurrentVersion', JavaVer);
      end;

    if Length( JavaVer ) > 0 then
    begin
	Log('An existing Java version ' + JavaVer + ' was found.')
    	if CompareVersion(JavaVer,MinVersion) >= 0 then
    	begin
            Log('The found Java version is not new enough. Required minimum ' + MinVersion);
    	    Result := true;
    	end;
    end
    else
        Log('No existing Java version was found.');
end;

function GetJavaDownloadUrl() : String;
var
    pagePath : String;
    pageContents : String;
    searchString : String;
    startIndex : Integer;
begin
    pagePath := AddBackslash(ExpandConstant('{tmp}')) + '\jre_page.htm'
    Log('Saving JRE download page to: ' + pagePath);
    idpDownloadFile(
        'https://www.java.com/en/download/manual.jsp',
        pagePath);
    LoadStringFromFile(pagePath, pageContents);
    if IsWin64 then
        searchString := 'Download Java software for Windows (64-bit)" href="'
    else
        searchString := 'Download Java software for Windows Offline" href="';
    pageContents := Copy(pageContents, Pos(searchString, pageContents) + Length(searchString), 1000)
    pageContents := Copy(pageContents, 0, Pos('"', pageContents) - 1)
    Log('Java download URL is ' + pageContents);
    Result := pageContents;
end;

function GetJavaDownloadPath(ParamUnused : String) : String;
begin
    Result := AddBackslash(ExpandConstant('{tmp}')) + '\jreinstall.exe';
end;

function NeedToInstallJava() : Boolean;
begin
    Result := (not CheckJRE('1.6'));
end;

procedure InitializeWizard();
var
    downloadURL : String;
begin
    if NeedToInstallJava then
    begin
        downloadURL := GetJavaDownloadUrl()
        idpAddFile(
            downloadURL,
            GetJavaDownloadPath(''));
        idpDownloadAfter(wpReady);
    end;
end;
