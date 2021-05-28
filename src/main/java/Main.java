import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        process();
    }

    private static void process() throws UnknownHostException {
        String vpcCidr = "10.190.0.0/20";
        int blockSize = 25;
        // cidr blocks should be sorted
        String[] u = {
                "10.190.0.0/22",
                "10.190.4.0/24",
                "10.190.5.0/25",
                "10.190.5.128/25",
                "10.190.6.0/24",
                "10.190.7.0/25",
                "10.190.7.128/25",
                "10.190.8.0/24",
                "10.190.9.0/25",
                "10.190.9.128/25",
//                "10.190.10.0/24",
                "10.190.11.0/24",
                "10.190.12.0/25",
                "10.190.12.128/25",
                "10.190.13.0/24",
                "10.190.14.0/25",
                "10.190.14.128/26",
                "10.190.14.192/27",
//                "10.190.14.224/27",
                "10.190.15.0/25",
                "10.190.15.128/26",
                "10.190.15.192/26"
        };



        List<String> usedCidrs = new ArrayList<String>(Arrays.asList(u));


        String leftPointer, rightPointer;
        boolean found = false;

        // the size of the block we are trying to find (eg: we want to find a /27 block inside the vpc

        leftPointer = getInfo(vpcCidr).getLowAddress() + "/32";
        rightPointer = addIp(leftPointer, (int) Math.pow(2, (32 - blockSize)) - 1);


        while (!found) {
            String leftPointerCidr = getAddressCidr(leftPointer, blockSize);
            String rightPointerCidr = getAddressCidr(rightPointer, blockSize);

            boolean used = false;

            // means valid CIDR
            if (leftPointerCidr.equals(rightPointerCidr)) {
                // iterate through all used cidrs, check if leftPointer~rightPointer falls inside any
                for (String cidr : usedCidrs) {
                    SubnetInfo info = getInfo(cidr);

                    if (isInRange(cidr, leftPointer, rightPointer)) {
                        used = true;
                    }

                    if (used) {
                        // since cidrs are in the sorted order, if we found a used cidr, we can move the left pointer
                        // to the end of the used cidr + 1
                        leftPointer = addIp(info.getHighAddress() + "/32", 1);
                        rightPointer = addIp(leftPointer, (int) Math.pow(2, (32 - blockSize)) - 1);

                        // also we can drop the cidr from the usedCidrs block
                        usedCidrs.remove(cidr);
                        break;
                    }
                }
            } else {
                leftPointer = addIp(getInfo(leftPointerCidr).getHighAddress() + "/32", 1);
                rightPointer = addIp(leftPointer, (int) Math.pow(2, (32 - blockSize)) - 1);
                continue;
            }

            if (!used) {
                System.out.println(leftPointerCidr);
                found = true;
            }

//            else {
//                leftPointer = addIp(rightPointer, 1);
//                rightPointer = addIp(leftPointer, (int) Math.pow(2, (32 - blockSize)) - 1);
//            }
        }
    }

    private static boolean isInRange(String cidr, String leftPointer, String rightPointer) {
        SubnetInfo cidrInfo = getInfo(cidr);

        try {
            int cidrFirstIp = ip2int(cidrInfo.getLowAddress());
            int cidrLastIp = ip2int(cidrInfo.getHighAddress());

            int leftIp = ip2int(getInfo(leftPointer).getAddress());
            int rightIp = ip2int(getInfo(rightPointer).getAddress());

            return !(rightIp < cidrFirstIp || cidrLastIp < leftIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static String getAddressCidr(String ip, int blockSize) {
        String startIp = getInfo(ip).getAddress();
        return getInfo(startIp + "/" + blockSize).getNetworkAddress() + "/" + blockSize;
    }

    private static SubnetInfo getInfo(String cidr) {
        SubnetUtils util = new SubnetUtils(cidr);
        util.setInclusiveHostCount(true);
        return util.getInfo();
    }

    private static int ip2int(String ip) throws UnknownHostException {
        InetAddress startIp = InetAddress.getByName(ip);
        int start = 0;
        for (byte b : startIp.getAddress()) {
            start = start << 8 | (b & 0xFF);
        }
        return start;
    }

    private static String int2ip(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24 & 0xff), (ip >> 16 & 0xff), (ip >> 8 & 0xff), (ip & 0xff));
    }

    private static String addIp(String startIp, int count) {
        String ip = getInfo(startIp).getAddress();

        try {
            int start = ip2int(ip);

            int added = start + count;
            String ipStr = int2ip(added);

            return ipStr + "/32";
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}

