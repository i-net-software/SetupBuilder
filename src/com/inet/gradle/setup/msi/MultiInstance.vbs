Option Explicit

Dim installDir, transform
installDir = Session.Property("INSTALLDIR")
log installDir

Dim FSO, SHELL
Set FSO = CreateObject("Scripting.FileSystemObject")
Set SHELL = CreateObject("WScript.Shell")

transform = Session.Property("TRANSFORMS")
log "TRANSFORMS: " & transform

Dim instanceNumber
If Not IsNull( transform ) And InStr( transform, ":Instance_" ) = 1 Then
    instanceNumber = Mid( transform, 11 )
    installDir = getInstallDir( instanceNumber )
    If isNull( installDir ) Or installDir = "" Then
        installDir = Session.Property("INSTALLDIR")
    Else
        Session.Property("INSTALLDIR") = installDir
    End If
Else
    installDir = FSO.GetAbsolutePathName( installDir ) ' normalize the case sensitivity
    If Right( installDir, 1 ) <> "\" Then
        installDir = installDir + "\"
    End If
    Session.Property("INSTALLDIR") = installDir

    instanceNumber = CStr( getInstanceID(installDir) )
End If

Dim parentPath, name, isStandardFolder
parentPath = FSO.GetParentFolderName( installDir )
log "parent path: " & parentPath
name = FSO.GetFileName( installDir )
log "name: " & name
isStandardFolder = isProgramFiles( parentPath )
log "program files: " & isStandardFolder

If Not isStandardFolder Then
    name = name & "_" & idCode( installDir )
End If
log "product name: " + name

Session.Property( "INSTANCE_NUMBER" ) = instanceNumber
Session.Property( "INSTANCE_ID" ) = "Instance_" & instanceNumber
log "instance: " + Session.Property( "INSTANCE_ID" )
Session.Property( "PRODUCT_NAME" ) = name


patchUpgradeDetected



' =====================
' write in the log file of the MSI
' =====================
Sub log( msg )
  Dim rec
  Set rec = Installer.CreateRecord(1)
  rec.StringData(1) = msg
  Session.Message &H04000000, rec
End Sub



' =====================
' If the given path is one of the (localized) program files paths 
' =====================
Function isProgramFiles( dirName )
    Dim environments, environment
    environments = Array( "%ProgramFiles%", "%ProgramFiles(x86)%", "%ProgramW6432%" )

    For Each environment In environments
        If StrComp( dirName, SHELL.ExpandEnvironmentStrings( environment ), 1) = 0 Then
            isProgramFiles = True
            Exit Function
        End If
    Next

    isProgramFiles = False
End Function



' =====================
' calculate an unique ID from the given string.
' =====================
Function idCode( ByVal str )
    If Right( str, 1 ) = "\" Then
        str = Left(str, Len(str) - 1)
    End If 
    idCode = LCase( Hex( hashCode( LCase( str ) ) ) )
End Function



' =====================
' The Java String.hashCode() method
' =====================
Function hashCode( str )
    Dim hash, length, i
    length = Len( str )
    hash = 0
    For i = 1 To length
        hash = 31 * hash + Asc( Mid( str, i, 1 ) )
        Do While hash > &H7FFFFFFF
            hash = hash + &H80000000 + &H80000000
        Loop
        Do While hash < &H80000000
            hash = hash - &H80000000 - &H80000000
        Loop
        hash = Clng(hash)
    Next
    hashCode = hash
End Function 



' =====================
' Get the ID (number) for the next free instance
' or the ID of an existing instance if the installDir matched.
' This function must be call before the "ProductName" property was changed.
' =====================
Function getInstanceID( installDir )
    Dim InstancesCount, Manufacturer, ProductName, i, instancesKey, reg, names

    InstancesCount = Session.Property( "InstancesCount" )
    ProductName = Session.Property( "ProductName" )
    Manufacturer = Session.Property( "Manufacturer" )
    instancesKey = "Software\" & Manufacturer & "\" & ProductName & "\Instances"

    Const HKLM = &H80000002
    const REG_SZ = 1
    Set reg = GetObject("winmgmts:!root\default:StdRegProv")
    reg.EnumKey HKLM, instancesKey, names

    If Not IsNull( names ) Then
        For i = 0 To UBound(names)
            Dim name
            name = names(i)
            If IsNumeric( name ) Then
                Dim dir, PackageCode
                reg.GetStringValue HKLM, instancesKey & "\" & name, "", dir
                If StrComp( installDir, dir, 1) = 0 Then
                    getInstanceID = CInt( name )
                    reg.GetStringValue HKLM, instancesKey & "\" & name, "PackageCode", PackageCode
                    log "Old PackageCode: " & Session.Property( "PackageCode" )
                    If Not IsNull( PackageCode ) Then
                        If PackageCode <> Session.Property( "PackageCode" ) Then
                            Session.Property("MSINEWINSTANCE") = "1"
                        End If
                    End If
                    Exit Function
                End If
            End If
        Next
    End If

    For i = 0 To InstancesCount - 1
        Dim value
        reg.GetStringValue HKLM, instancesKey & "\" & i, "", value
        If IsNull( value ) Then
            getInstanceID = i
            Session.Property("MSINEWINSTANCE") = "1"
            Exit Function
        End If
    Next

    MsgBox "To many instances installed. The maximum of " & InstancesCount & " is already installed."
    getInstanceID = -1 ' this should produce an error in the calling setup because the ID does not exists
End Function



' =====================
' Get the install directory for the given ID
' =====================
Function getInstallDir( instanceID )
    Dim Manufacturer, ProductName, reg, value

    ProductName = Session.Property( "ProductName" )
    Manufacturer = Session.Property( "Manufacturer" )

    Const HKLM = &H80000002

    Set reg = GetObject("winmgmts:!root\default:StdRegProv")

    reg.GetStringValue HKLM, "Software\" & Manufacturer & "\" & ProductName & "\Instances\" & instanceID, "", value
    getInstallDir = value
End Function


' =====================
' Patch the upgarde properties. Only the current instance should replaced.
' All other instances was detected but should not replaced.
' =====================
Function patchUpgradeDetected()
    Dim i, InstancesCount
    InstancesCount = Session.Property( "InstancesCount" )
    For i = 0 To InstancesCount - 1
        If CStr(i) <> instanceNumber Then
            Session.Property( "WIX_UPGRADE_DETECTED_" & i ) = ""
        End If
    Next
End Function