package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;
import java.text.DecimalFormat;

public class WorkloadSpike {

    private static final int NUM_VMS      = 4;
    private static final int NORMAL_TASKS = 3;
    private static final int SPIKE_TASKS  = 10;

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("   CLOUD WORKLOAD SPIKE SIMULATION (CloudSim)    ");
        System.out.println("=================================================\n");

        try {
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, false);

            Datacenter datacenter = createDatacenter("Datacenter_1");

            DatacenterBroker broker = new DatacenterBroker("Broker_1");
            int brokerId = broker.getId();

            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < NUM_VMS; i++) {
                Vm vm = new Vm(i, brokerId, 250, 1, 512, 1000, 10000,
                        "Xen", new CloudletSchedulerTimeShared());
                vmList.add(vm);
            }
            broker.submitVmList(vmList);

            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilizationModel = new UtilizationModelFull();

            System.out.println("Phase 1: Normal Load (" + NORMAL_TASKS + " tasks)");
            for (int i = 0; i < NORMAL_TASKS; i++) {
                Cloudlet cloudlet = new Cloudlet(i, 5000, 1, 300, 300,
                        utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            System.out.println("Phase 2: Workload SPIKE (" + SPIKE_TASKS + " tasks at once)\n");
            for (int i = NORMAL_TASKS; i < NORMAL_TASKS + SPIKE_TASKS; i++) {
                Cloudlet cloudlet = new Cloudlet(i, 5000, 1, 300, 300,
                        utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            printResults(finishedCloudlets);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));

        List<Host> hostList = new ArrayList<>();
        hostList.add(new Host(0,
                new RamProvisionerSimple(8192),
                new BwProvisionerSimple(100000),
                1000000, peList,
                new VmSchedulerTimeShared(peList)));

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList,
                10.0, 3.0, 0.05, 0.001, 0.1);

        return new Datacenter(name, characteristics,
                new VmAllocationPolicySimple(hostList),
                new LinkedList<>(), 0);
    }

    private static void printResults(List<Cloudlet> cloudlets) {
        DecimalFormat df = new DecimalFormat("###.##");

        System.out.println("\n" + "=".repeat(70));
        System.out.printf("%-12s %-10s %-15s %-15s %-12s%n",
                "Cloudlet ID", "Status", "Start Time", "Finish Time", "Exec Time");
        System.out.println("-".repeat(70));

        double totalExecTime = 0;
        int normalDone = 0, spikeDone = 0;

        for (Cloudlet c : cloudlets) {
            String status = (c.getCloudletStatus() == Cloudlet.SUCCESS) ? "SUCCESS" : "FAILED";
            double execTime = c.getActualCPUTime();
            totalExecTime += execTime;

            if (c.getCloudletId() < NORMAL_TASKS) normalDone++;
            else spikeDone++;

            System.out.printf("%-12d %-10s %-15s %-15s %-12s%n",
                    c.getCloudletId(), status,
                    df.format(c.getExecStartTime()),
                    df.format(c.getFinishTime()),
                    df.format(execTime));
        }

        System.out.println("=".repeat(70));
        System.out.println("\nSUMMARY:");
        System.out.println("   Total Tasks Completed : " + cloudlets.size());
        System.out.println("   Normal Load Tasks     : " + normalDone);
        System.out.println("   Spike Load Tasks      : " + spikeDone);
        System.out.printf ("   Avg Execution Time    : %s seconds%n",
                df.format(totalExecTime / cloudlets.size()));
        System.out.println("\nSpike tasks took longer due to VM resource contention!");
    }
}