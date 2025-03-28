package autoutil.controllers;


public class sqiud {
    double p, i, d;
    public sqiud() {
        p=0;
        i=0;
        d=0;
    }
    public void setPID(double p) {
        this.p = p;
    }
    public double calculate(double setpoint, double current) {
        return Math.sqrt(Math.abs((setpoint-current)*p))*Math.signum(setpoint-current);
    }
}