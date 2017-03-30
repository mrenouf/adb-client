package com.bitgrind.android.usb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UsbIdsLoader {

    private static final Pattern ID = Pattern.compile("^\t?([a-f0-9]{4})\\s+(.*)$");

    static class UsbId {
        final int vendorId;
        final int productId;
        final String vendorName;
        final String productName;

        public UsbId(int vendorId, int productId, String vendorName, String productName) {
            this.vendorId = vendorId;
            this.productId = productId;
            this.vendorName = vendorName;
            this.productName = productName;
        }



        @Override
        public String toString() {
            return String.format("{%04x:%s}: %s: %s",
                    vendorId, (productId == -1) ? "****" : String.format("%04x", productId),
                    vendorName, productName);
        }
    }

    static List<UsbId> usbIds() {
        List<UsbId> list = new ArrayList<>();
        try {
            final InputStream input = UsbIdsLoader.class.getResourceAsStream("usb.ids");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            int productId = -1;
            int vendorId = -1;
            String productName = "";
            String vendorName = "";
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = ID.matcher(line);
                if (matcher.matches()) {
                    if (line.startsWith("\t")) {
                        productId = Integer.parseInt(matcher.group(1), 16);
                        productName = matcher.group(2);
                        list.add(new UsbId(vendorId, productId, vendorName, productName));
                    } else {
                        if (vendorId != -1 && productId == -1) {
                            list.add(new UsbId(vendorId, productId, vendorName, productName));
                        }
                        vendorId = Integer.parseInt(matcher.group(1), 16);
                        vendorName = matcher.group(2);
                        productId = -1;
                    }
                }
            }
        } catch (IOException e) {
        }
        return list;
    }



    public static void main(String[] args) throws IOException {
        for (UsbId id : usbIds()) {
            System.out.println(id);
        }
    }

}
