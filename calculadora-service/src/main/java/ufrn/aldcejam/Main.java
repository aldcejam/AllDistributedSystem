package ufrn.aldcejam;

import ufrn.aldcejam.infra.communication.ServerManagement;

public class Main {
    public static void main(String[] args) throws Exception {
        ServerManagement serverManagement = new ServerManagement();
        serverManagement.start();
    }
}