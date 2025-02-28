package util.purepursuit;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.atan2;
import static java.lang.Math.hypot;
import static java.lang.Math.pow;

import static mathutil.MathFunc.PI;

import com.acmerobotics.roadrunner.geometry.Pose2d;

import java.util.ArrayList;

public class PurePursuitUtil {

    private static int moveSegment = 1;
    private static int headingSegment=1;

    private static int touchCounter=0;

//    private static boolean ending = false;

    private static boolean closeEnough = false;


    private static double pathLength;
    private static Pose2d check = new Pose2d(0,0);

    public static int getMoveSegment(){
        return moveSegment;
    }
    public static int getHeadingSegment() { return headingSegment; }
    public static void setPathLength(double pl){
        pathLength = pl;
    }
    public static void updateMoveSegment(int n){
        moveSegment = n;
    }
    public static void updateHeadingSegment(int n){
        headingSegment = n;
    }
    public static Pose2d getInt(){
        return check;
    }
    public static ArrayList<Pose2d> lineCircleIntersection(Pose2d circleCenter, double radius, Pose2d linePoint1, Pose2d linePoint2, boolean acceptBefore, boolean acceptAfter) {
        double discTolerance = 0.01;
        double m1;
        ArrayList<Pose2d> allPoints = new ArrayList<>();

        double vertShift = 0;

        // no verticals
        if (linePoint2.getX() == linePoint1.getX()) {
            vertShift = 0.001;
        }

        linePoint1 = new Pose2d(linePoint1.getX() + vertShift, linePoint1.getY());

        // Handle vertical line
        if (linePoint2.getX() - linePoint1.getX() != 0) {
            m1 = (linePoint2.getY() - linePoint1.getY()) / (linePoint2.getX() - linePoint1.getX());
        } else {
            if(linePoint2.getY()>linePoint1.getY()){
                allPoints.add(new Pose2d(circleCenter.getX(), circleCenter.getY()+radius, angleWrap(PI/2, circleCenter.getHeading())));
            }else {
                allPoints.add(new Pose2d(circleCenter.getX(), circleCenter.getY()-radius, angleWrap(-PI/2, circleCenter.getHeading())));
            }
            return allPoints;
        }

        double x1 = linePoint1.getX() - circleCenter.getX();
        double y1 = linePoint1.getY() - circleCenter.getY();

        // Quadratic coefficients of the solution to the intersection of the line segment and circle
        double A = 1 + pow(m1, 2);
        double B = m1 * y1 - pow(m1, 2) * x1;
        double C = pow(y1, 2) - 2 * m1 * y1 * x1 + pow(m1, 2) * pow(x1, 2) - pow(radius, 2);

        double disc = Math.sqrt(pow(B, 2) -  A * C);


        try {
            if (disc > discTolerance) {
                // First root
                double xroot1 = (-B - disc) / ( A);
                double yroot1 = yCalculator(x1, y1, m1, xroot1);
                xroot1 += circleCenter.getX();
                yroot1 += circleCenter.getY();

                // Second root
                double xroot2 = (-B + disc) / (A);
                double yroot2 = yCalculator(x1, y1, m1, xroot2);
                xroot2 += circleCenter.getX();
                yroot2 += circleCenter.getY();

                double withinSegment1 = segmentPercentage(linePoint1, linePoint2, xroot1,yroot1);
                double withinSegment2 = segmentPercentage(linePoint1, linePoint2, xroot2,yroot2);

                if ((acceptBefore && withinSegment1<0)
                        ||(acceptAfter && withinSegment1>1)
                        || (withinSegment1<1 && withinSegment1>0)) {
                    check = new Pose2d(xroot1, yroot1, heading(circleCenter, xroot1, yroot1));
                    allPoints.add(new Pose2d(xroot1, yroot1, heading(circleCenter, xroot1, yroot1)));
                }
                if ((acceptBefore && withinSegment2<0)
                        ||(acceptAfter && withinSegment2>1)
                        || (withinSegment2<1 && withinSegment2>0)) {

                    allPoints.add(new Pose2d(xroot2, yroot2, heading(circleCenter, xroot2, yroot2)));
                }
            } else if (disc >= 0 && disc <= discTolerance) {
                double xroot = -B / A;
                double yroot = yCalculator(x1, y1, m1, xroot);
                xroot += circleCenter.getX();
                yroot += circleCenter.getY();
                check = new Pose2d(xroot, yroot, heading(circleCenter, xroot, yroot));
                allPoints.add(new Pose2d(xroot, yroot, heading(circleCenter, xroot, yroot)));
            }else{
                Pose2d pt = recoveryPt(linePoint1, linePoint2, circleCenter);
                //allPoints.add(pt);
            }
        } catch (Exception e) {
            // Handle exceptions
        }

        return allPoints;
    }
    //checks if a point is on a line segment
    public static double segmentPercentage(Pose2d start, Pose2d end, double x, double y){
        double dist = hypot((end.getX()-start.getX()), (end.getY()-start.getY()));
        double distBefore = hypot((x-start.getX()), (y-start.getY()));
        double distAfter = hypot((end.getX()-x), (end.getY()-y));
        //if the distance after is greater than the total distance, you're before the 1st waypt
        if(distAfter>dist){
            return -distBefore/dist;
        }
        //otherwise the formula is the same
        return distBefore/dist;

    }
    public static Pose2d followMe(ArrayList<Pose2d> path, Pose2d robotLocation, double followRadius, Pose2d lastPoint, boolean heading ) {


        // stores all intersections, valid or not, for the entire path
        ArrayList<Pose2d> allIntersections = new ArrayList<>();
        //segment based view
        if (heading) {
            if (headingSegment == path.size() - 1) {
                Pose2d wayPt1 = path.get(headingSegment - 1);
                Pose2d wayPt2 = path.get(headingSegment);

                ArrayList<Pose2d> intersections = lineCircleIntersection(robotLocation, followRadius, wayPt1, wayPt2, false, true);
                if (intersections.size() == 2) {
                    if (segmentPercentage(wayPt1, wayPt2, intersections.get(0).getX(), intersections.get(0).getY()) > segmentPercentage(wayPt1, wayPt2, intersections.get(1).getX(), intersections.get(1).getY())) {
                        allIntersections.add(intersections.get(0));
                    } else {
                        allIntersections.add(intersections.get(1));
                    }

                }
                // if there's only one intersection then save that
                else if (intersections.size() == 1) {
                    allIntersections.add(intersections.get(0));
                }

            } else {
                Pose2d wayPt1 = path.get(headingSegment - 1);
                Pose2d wayPt2 = path.get(headingSegment);
                Pose2d wayPt3 = path.get(headingSegment + 1);

                ArrayList<Pose2d> intersections = lineCircleIntersection(robotLocation, followRadius, wayPt1, wayPt2, false, false);
                ArrayList<Pose2d> fintersections = lineCircleIntersection(robotLocation, followRadius, wayPt2, wayPt3, false, false);

                if (fintersections.size() != 0) {
                    if (fintersections.size() == 1) {
                        allIntersections.add(fintersections.get(0));
                    } else if (fintersections.size() == 2) {
                        if (hypot(wayPt3.getX() - fintersections.get(0).getX(), wayPt3.getY() - fintersections.get(0).getY()) > hypot(wayPt3.getX() - fintersections.get(1).getX(), wayPt3.getY() - fintersections.get(1).getY())) {
                            allIntersections.add(fintersections.get(1));
                        } else {
                            allIntersections.add(fintersections.get(0));
                        }

                    }
                    headingSegment++;

                } else {
                    // if 2 valid intersections, then save the one that's more along the line
                    if (intersections.size() == 2) {
                        if (hypot(wayPt2.getX() - intersections.get(0).getX(), wayPt2.getY() - intersections.get(0).getY()) > hypot(wayPt2.getX() - intersections.get(1).getX(), wayPt2.getY() - intersections.get(1).getY())) {
                            allIntersections.add(intersections.get(1));
                        } else {
                            allIntersections.add(intersections.get(0));
                        }
                    }

                    // if there's only one intersection then save that
                    else if (intersections.size() == 1) {
                        allIntersections.add(intersections.get(0));
                    }

                    // if you're closer to the last point than follow radius then add it to the list, unless you're extending
                    else if ((hypot(wayPt2.getX() - robotLocation.getX(), wayPt2.getY() - robotLocation.getY()) < followRadius)) {
                        allIntersections.add(wayPt2);
                    }

                }
            }
        }

        if (!heading) {
            if (moveSegment == path.size() - 1) {
                Pose2d wayPt1 = path.get(moveSegment - 1);
                Pose2d wayPt2 = path.get(moveSegment);


                ArrayList<Pose2d> intersections = lineCircleIntersection(robotLocation, followRadius, wayPt1, wayPt2, false, false);
                if (pathLength < followRadius) {
                    allIntersections.add(wayPt2);
                } else if (intersections.size() == 2) {
                    if (hypot(wayPt2.getX() - intersections.get(0).getX(), wayPt2.getY() - intersections.get(0).getY()) > hypot(wayPt2.getX() - intersections.get(1).getX(), wayPt2.getY() - intersections.get(1).getY())) {
                        allIntersections.add(intersections.get(1));
                    } else {
                        allIntersections.add(intersections.get(0));
                    }
                }

                // if there's only one intersection then save that
                else if (intersections.size() == 1) {
                    allIntersections.add(intersections.get(0));
                }
            } else {
                Pose2d wayPt1 = path.get(moveSegment - 1);
                Pose2d wayPt2 = path.get(moveSegment);
                Pose2d wayPt3 = path.get(moveSegment + 1);

                ArrayList<Pose2d> intersections = lineCircleIntersection(robotLocation, followRadius, wayPt1, wayPt2, false, false);
                ArrayList<Pose2d> fintersections = lineCircleIntersection(robotLocation, followRadius, wayPt2, wayPt3, false, false);

                if (fintersections.size() != 0) {
                    if (fintersections.size() == 1) {
                        allIntersections.add(fintersections.get(0));
                    } else if (fintersections.size() == 2) {
                        if (hypot(wayPt3.getX() - fintersections.get(0).getX(), wayPt3.getY() - fintersections.get(0).getY()) > hypot(wayPt3.getX() - fintersections.get(1).getX(), wayPt3.getY() - fintersections.get(1).getY())) {
                            allIntersections.add(fintersections.get(1));
                        } else {
                            allIntersections.add(fintersections.get(0));
                        }

                    }
                    moveSegment++;

                } else {
                    // if 2 valid intersections, then save the one that's more along the line
                    if (intersections.size() == 2) {
                        if (hypot(wayPt2.getX() - intersections.get(0).getX(), wayPt2.getY() - intersections.get(0).getY()) > hypot(wayPt2.getX() - intersections.get(1).getX(), wayPt2.getY() - intersections.get(1).getY())) {
                            allIntersections.add(intersections.get(1));
                        } else {
                            allIntersections.add(intersections.get(0));
                        }
                    }

                    // if there's only one intersection then save that
                    else if (intersections.size() == 1) {
                        allIntersections.add(intersections.get(0));
                    }

                    // if you're closer to the last point than follow radius then add it to the list, unless you're extending
                    else if ((hypot(wayPt2.getX() - robotLocation.getX(), wayPt2.getY() - robotLocation.getY()) < followRadius)) {
                        allIntersections.add(wayPt2);
                    }

                }
            }


            // if we didn't find anything return robot location



        }
        if (allIntersections.size() == 0) {
            return lastPoint;
        }

        return allIntersections.get(allIntersections.size() - 1);
    }
    public static boolean passedWayPt(Pose2d robotLocation, Pose2d wayPt, Double radius){
        return hypot((robotLocation.getY()- wayPt.getY()),(robotLocation.getX()- wayPt.getX())) <= radius;
    }
    public static Pose2d recoveryPt(Pose2d waypt1, Pose2d waypt2, Pose2d robotLocation){
        if(robotLocation.getX()<min(waypt1.getX(), waypt2.getX())){
            Pose2d newPt = min(waypt1.getX(), waypt2.getX()) == waypt1.getX() ? waypt1:waypt2;
            return new Pose2d(newPt.getX(), newPt.getY(), heading(robotLocation,newPt.getX(),newPt.getY()));
        }else if(robotLocation.getX()>max(waypt1.getX(), waypt2.getX())){
            Pose2d newPt = max(waypt1.getX(), waypt2.getX()) == waypt1.getX() ? waypt1:waypt2;
            return new Pose2d(newPt.getX(), newPt.getY(), heading(robotLocation,newPt.getX(),newPt.getY()));
        }else if(robotLocation.getX()<=max(waypt1.getX(), waypt2.getX())&&robotLocation.getX()>=min(waypt1.getX(), waypt2.getX())){
            double m;
            if (waypt2.getX() - waypt1.getX() != 0) {
                m = (waypt2.getY() - waypt1.getY()) / (waypt2.getX() - waypt1.getX());
                double b = (waypt2.getY()-waypt1.getY()) - m*(waypt2.getX()-waypt1.getX());
                double x1 = robotLocation.getX();
                double y1 = robotLocation.getY() - b;
                if(m==0){
                    b = waypt1.getY();
                    return new Pose2d(robotLocation.getX(), b,heading(robotLocation, robotLocation.getX(), b));
                }else{
                    double rootx = (m*(y1+x1*(1/m)))/(pow(m,2)+1);
                    double rooty = m*rootx + b;
                    return new Pose2d(rootx, rooty, heading(robotLocation, rootx, rooty));
                }
            } else {
                return new Pose2d(waypt1.getX(), robotLocation.getY(), heading(robotLocation, waypt1.getX(), robotLocation.getY()));
            }
        }else{
            Pose2d pt = hypot(robotLocation.getX()-waypt1.getX(), robotLocation.getY()-waypt1.getY()) < hypot(robotLocation.getX()-waypt2.getX(), robotLocation.getY()-waypt2.getY()) ? waypt1 : waypt2;
            return new Pose2d(pt.getX(), pt.getY(), heading(robotLocation, pt.getX(), pt.getY()));
        }
    }
    public static double yCalculator(double x1, double y1, double m, double rootx) {
        return m * (rootx - x1) + y1;
    }
    public static double heading(Pose2d robotLocation, double targetX, double targetY){
        double vectorX = targetX - robotLocation.getX();
        double vectorY = targetY - robotLocation.getY();

        double angle = atan2(vectorY, vectorX);

        return angleWrap(angle, robotLocation.getHeading());
    }
    public static double angleWrap(double targetAngle, double currentAngle){
        if(targetAngle-currentAngle>Math.PI){
            targetAngle -= Math.PI;
            return targetAngle;
        }else if(targetAngle-currentAngle<-Math.PI){
            targetAngle += Math.PI;
            return targetAngle;
        }else{
            return targetAngle;
        }
    }
    public static double distanceTo(Pose2d robot, Pose2d target){
        return hypot(robot.getX()-target.getX(), robot.getY()-target.getY());
    }
}