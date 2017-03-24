package com.bitgrind.android.usb;

import org.usb4java.*;

import java.io.FileDescriptor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mrenouf on 3/14/17.
 */
public class UsbTest {
    public static void main(String[] args) throws InterruptedException {
        Context context = new Context();
        int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);

        AtomicInteger count = new AtomicInteger();

        HotplugCallback callback = new HotplugCallback() {
            @Override
            public int processEvent(Context context, Device device, int event, Object userData) {
                if (event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED) {
                    DeviceDescriptor desc = new DeviceDescriptor();
                    if (LibUsb.getDeviceDescriptor(device, desc) == 0) {
                        System.out.format("----------%04x %04x----------", desc.idVendor(), desc.idProduct());
                        ConfigDescriptor cfg = new ConfigDescriptor();
                        int err;
                        if ((err = LibUsb.getActiveConfigDescriptor(device, cfg)) == 0) {
                            DeviceHandle handle = new DeviceHandle();
                            if ((err = LibUsb.open(device, handle)) == 0) {
                                for (Interface i : cfg.iface()) {
                                    for (InterfaceDescriptor id : i.altsetting()) {
                                        System.out.println(LibUsb.getStringDescriptor(handle, id.iInterface()));
                                    }
                                }
                                LibUsb.close(handle);
                            } else {
                                switch (err) {
                                    case LibUsb.ERROR_ACCESS:
                                        System.out.println("No access to open.");
                                        break;
                                    case LibUsb.ERROR_NO_MEM:
                                        System.out.println("Could not allocate memory.");
                                        break;
                                    case LibUsb.ERROR_NO_DEVICE:
                                        System.out.println("Device is gone.");
                                        break;
                                }
                            }
                        } else {
                            if (err == LibUsb.ERROR_NOT_FOUND) {
                                System.out.println("Device is ot configured.");
                            } else {
                                System.out.println("Unknown error: code " + err);
                            }
                        }
                    }
                }
                if (event == LibUsb.HOTPLUG_EVENT_DEVICE_LEFT) {
                    DeviceDescriptor desc = new DeviceDescriptor();
                    LibUsb.getDeviceDescriptor(device, desc);
                    System.out.format("----------%04x %04x----------", desc.idVendor(), desc.idProduct());
                    ConfigDescriptor cfg = new ConfigDescriptor();
                    LibUsb.getActiveConfigDescriptor(device, cfg);
                    DeviceHandle handle = new DeviceHandle();
                    int open = LibUsb.open(device, handle);
                    for (Interface i : cfg.iface()) {
                        for (InterfaceDescriptor id : i.altsetting()) {
                            System.out.println(LibUsb.getStringDescriptor(handle, id.iInterface()));
                        }
                    }
                    LibUsb.close(handle);
                }
                count.incrementAndGet();
                return 0;
            }
        };
        HotplugCallbackHandle handle = new HotplugCallbackHandle();

        LibUsb.hotplugRegisterCallback(context,
                LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED | LibUsb.HOTPLUG_EVENT_DEVICE_LEFT,
                LibUsb.HOTPLUG_ENUMERATE,LibUsb.HOTPLUG_MATCH_ANY,
                LibUsb.HOTPLUG_MATCH_ANY,
                LibUsb.HOTPLUG_MATCH_ANY,
                callback,
                "DATA",
                handle);

        System.out.println(LibUsb.hasCapability(LibUsb.CAP_HAS_HOTPLUG));

        while (count.get() < 2) {
            LibUsb.handleEvents(context);
            Thread.sleep(10);
        }
        LibUsb.exit(context);
    }

    // Ported from libusb-glue.c - probe_device_descriptor
    boolean isMtpDevice(Device device) {

        return false;
    }
}
