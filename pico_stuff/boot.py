import usb_cdc

# usb_cdc.disable()   # Disable both serial devices.

# usb_cdc.enable(console=True, data=False)   # Enable just console
                                           # (the default setting)

usb_cdc.enable(console=True, data=True)    # Enable console and data
