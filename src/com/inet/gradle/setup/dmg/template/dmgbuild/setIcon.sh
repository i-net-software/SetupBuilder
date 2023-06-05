#!/bin/sh
# Sets an icon on file or directory
# Usage setIcon.sh iconimage.jpg /path/to/[file|folder]
# see https://stackoverflow.com/questions/8371790/how-to-set-icon-on-file-or-directory-using-cli-on-os-x

iconSource=$1
iconDestination=$2
icon=/tmp/`basename "$iconSource"`
rsrc=/tmp/icon.rsrc

# Create icon from the iconSource
cp "$iconSource" "$icon"

# Add icon to image file, meaning use itself as the icon
sips -i "$icon"

# Take that icon and put it into a rsrc file
DeRez -only icns "$icon" > "$rsrc"

# Apply the rsrc file to
SetFile -a C "$iconDestination"

if [ -f "$iconDestination" ]; then
    # Destination is a file
    Rez -append "$rsrc" -o "$iconDestination"
elif [ -d "$iconDestination" ]; then
    # Destination is a directory
    # Create the magical Icon\r file
    touch "$iconDestination/$'Icon\r'"
    Rez -append "$rsrc" -o "$iconDestination/Icon?"
    SetFile -a V "$iconDestination/Icon?"
fi

# Sometimes Finder needs to be reactivated
#osascript -e 'tell application "Finder" to quit'
#osascript -e 'delay 2'
#osascript -e 'tell application "Finder" to activate'

rm "$rsrc" "$icon"
