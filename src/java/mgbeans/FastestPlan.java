/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mgbeans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import org.hibernate.Session;
import pojos.Airport;
import pojos.Route;

/**
 *
 * @author metroeger
 */
@ManagedBean
@RequestScoped
public class FastestPlan {

    private List<Airport> airports;
    private List<Route> routes;
    private List<Route> tempRow;
    private Airport firstAirport;
    private Airport actualAirport;
    private Airport lastAirport;
    private int takeAway;
    private int actualKm;
    private List<Airport> way;
    private List<Route> finalWay;
    private boolean weighted;
    private String choosenOrigin;
    private String choosenDestination;
    private Map<String, Airport> airportMap;
    private boolean showDestinations;
    private String answer;

    public FastestPlan() {
        Session session = hibernate.HibernateUtil.getSessionFactory().openSession();
        airports = session.createQuery("FROM Airport").list();
        routes = session.createQuery("FROM Route").list();
        session.close();

        choosenOrigin = "";
        choosenDestination = "";

        showDestinations = false;
        firstAirport = null;
        lastAirport = null;

        airportMap = new HashMap<>();
        for (Airport ap : airports) {
            airportMap.put(ap.getCode(), ap);
        }
    }

    public void chooseOrigin(String origin) {
        firstAirport = airportMap.get(origin.substring(1, 4));
        showDestinations = true;
    }

    public void chooseDestination(String destination) {
        lastAirport = airportMap.get(destination.substring(1, 4));
    }

    public void clear() {
        try{
        firstAirport = null;
        lastAirport = null;
        way.clear();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void start() {
        tempRow = new ArrayList<>();
        for (int i = 0; i < airports.size(); i++) {
            if (firstAirport == airports.get(i)) {
                tempRow.add(new Route(firstAirport, airports.get(i), 0, true));
            } else {
                tempRow.add(new Route(firstAirport, airports.get(i), Integer.MAX_VALUE, false));
            }
        }
        actualAirport = new Airport(firstAirport.getCode(), firstAirport.getName(), firstAirport.getCountryCode());
    }

    public void fillTempRow() {
        for (int i = 0; i < airports.size(); i++) {
            Route v = findRoute(actualAirport, airports.get(i));
            //if (v != null && !v.isVisited()) {
            if (v != null) {
                for (int j = 0; j < tempRow.size(); j++) {
                    if (!tempRow.get(j).isVisited()) {
                        if (tempRow.get(j).getDestination() == airports.get(i) && v.getWeight() + takeAway < tempRow.get(i).getWeightNow()) {
                            tempRow.get(i).setWeightNow(v.getWeight() + takeAway);
                            tempRow.get(i).setOrigin(actualAirport);
                        }
                    }
                }

            }
        }
    }

    public void fillFirstTempRow() {
        for (int i = 0; i < tempRow.size(); i++) {
            for (int j = 0; j < routes.size(); j++) {
                // if this route exists
                if (tempRow.get(i).getOrigin() == routes.get(j).getOrigin() && tempRow.get(i).getDestination() == routes.get(j).getDestination()
                        || tempRow.get(i).getDestination() == routes.get(j).getOrigin() && tempRow.get(i).getOrigin() == routes.get(j).getDestination()) {
                    //set tempRow's route's weigthNow to route weight 
                    tempRow.get(i).setWeightNow(routes.get(j).getWeight());
                }
            }
        }
    }

//    public Route findRoute(Route route) {
//
//        for (Route r : routes) {
//            if (route.getOrigin() == r.getOrigin() && route.getDestination() == r.getDestination()
//                    || route.getDestination() == r.getOrigin() && route.getOrigin() == r.getDestination()) {
//                if (!r.isVisited()) {
//                    return r;
//                }
//            }
//        }
//        return null;
//    }

    public Route findRoute(Airport a, Airport b) {
        for (Route r : routes) {
            if (a == r.getOrigin() && b == r.getDestination()
                    || b == r.getOrigin() && a == r.getDestination()) {
                return r;
            }
        }
        return null;
    }

    public void findMin() {
        int min = Integer.MAX_VALUE;
        Route re = null;

        for (Route r : tempRow) {
            if (r.getWeightNow() < min && !r.isVisited()) {
                min = r.getWeightNow();
                re = r;
            }
        }
        takeAway = min;

        actualAirport = re.getDestination();
        re.setVisited(true);
    }

    public boolean isAllVisited() {
        for (Route r : tempRow) {
            if (!r.isVisited()) {
                return false;
            }
        }
        return true;
    }

    public void calculateWay(Airport o, Airport d) {
        way = new ArrayList<>();
        finalWay = new ArrayList<>();

        Airport dest = d;

        do {
            for (Route r : tempRow) {
                if (r.getDestination() == dest) {
                    way.add(r.getDestination());
                    finalWay.add(r);
                    dest = r.getOrigin();
                }
            }
            if (dest == o) {
                way.add(dest);
            }
        } while (!dest.getName().equals(o.getName()));

    }

    public void getLeastStops() {
        for (Route r : routes) {
            r.setWeight(1);
        }
        weighted = false;
    }

    public void findFinalRoute(String origin, String destination) {
        chooseOrigin(origin);
        chooseDestination(destination);
        setWeightToOne();

        boolean isAll = true;
        start();
        fillFirstTempRow();

        do {
            findMin();
            fillTempRow();
            isAll = isAllVisited();
        } while (isAll == false);

        calculateWay(firstAirport, lastAirport);
    }

    public void setWeightToOne() {
        for (Route r : routes) {
            r.setWeight(1);
        }
    }

    public String show() {
        answer = "";
        for (int i = way.size() - 1; i >= 0; i--) {
            if (i > 0) {
                if (way.get(i) != way.get(i - 1)) {
                    answer += way.get(i) + "->";
                }
            } else {
                answer += way.get(i);
            }
        }
        return answer;
    }

    public List<Airport> getAirports() {
        return airports;
    }

    public void setAirports(List<Airport> airports) {
        this.airports = airports;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public List<Route> getTempRow() {
        return tempRow;
    }

    public void setTempRow(List<Route> tempRow) {
        this.tempRow = tempRow;
    }

    public Airport getFirstAirport() {
        return firstAirport;
    }

    public void setFirstAirport(Airport firstAirport) {
        this.firstAirport = firstAirport;
    }

    public int getTakeAway() {
        return takeAway;
    }

    public void setTakeAway(int takeAway) {
        this.takeAway = takeAway;
    }

    public Airport getActualAirport() {
        return actualAirport;
    }

    public void setActualAirport(Airport actualAirport) {
        this.actualAirport = actualAirport;
    }

    public int getActualKm() {
        return actualKm;
    }

    public void setActualKm(int actualKm) {
        this.actualKm = actualKm;
    }

    public List<Airport> getWay() {
        return way;
    }

    public void setWay(List<Airport> way) {
        this.way = way;
    }

    public Airport getLastAirport() {
        return lastAirport;
    }

    public void setLastAirport(Airport lastAirport) {
        this.lastAirport = lastAirport;
    }

    public List<Route> getFinalWay() {
        return finalWay;
    }

    public void setFinalWay(List<Route> finalWay) {
        this.finalWay = finalWay;
    }

    public boolean isWeighted() {
        return weighted;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public String getChoosenOrigingetChoosenOrigin() {
        return choosenOrigin;
    }

    public void setChoosenAirport(String choosenAirport) {
        this.choosenOrigin = choosenAirport;
    }

    public Map<String, Airport> getAirportMap() {
        return airportMap;
    }

    public void setAirportMap(Map<String, Airport> airportMap) {
        this.airportMap = airportMap;
    }

    public boolean isShowDestinations() {
        return showDestinations;
    }

    public void setShowDestinations(boolean showDestinations) {
        this.showDestinations = showDestinations;
    }

    public String getChoosenOrigin() {
        return choosenOrigin;
    }

    public void setChoosenOrigin(String choosenOrigin) {
        this.choosenOrigin = choosenOrigin;
    }

    public String getChoosenDestination() {
        return choosenDestination;
    }

    public void setChoosenDestination(String choosenDestination) {
        this.choosenDestination = choosenDestination;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

}
