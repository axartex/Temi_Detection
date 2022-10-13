package com.example.temi_app_marco;

public class SoundSource {
    private double azimuth; // in interval ]-180, 180]
    private double elevation; // in interval ]0, 90]
    private double confidence;
    private static final double BIAS = 45; //used to transform ODAS 0º into TEMI 0º

    public SoundSource(double azimuth_in, double elevation_in){
        this.azimuth = azimuth_in;
        this.elevation = elevation_in;
        this.confidence = 0;
    }

    public static SoundSource getSource(double x, double y, double z) {
        double azimuth = (Math.atan2(y,x) * 180) / Math.PI;
        double elevation = (Math.atan2(z, Math.sqrt(x*x + y*y)) * 180) / Math.PI;
        azimuth = azimuth - BIAS; //TODO: TEST THIS WITH TURNING
        azimuth = tools.wrapTo180(azimuth);
        return new SoundSource(azimuth, elevation);
    }

    public double getAzimuth(){
        return this.azimuth;
    }

    public double getElevation(){
        return this.elevation;
    }

    public void setElevation(double newElevation){
        this.elevation = newElevation;
    }

    public double getConfidence() {
        return confidence;
    }

    public double setConfidence(double u, double alpha){
        this.confidence = alpha * this.confidence + (1-alpha)*u;
        return this.confidence;
    }

    public String toString(){
        return "Elevation " + String.valueOf(getElevation()) + "\nAzimuth " +
                String.valueOf(getAzimuth()) + "\nConfidence " + String.valueOf(getConfidence());
    }

    /*public void updateSource(double x, double y, double z){
        this.azimuth = Math.atan2(y,x);
        this.elevation = Math.atan2(z, Math.sqrt(x*x + y*y));
    }*/

    /*public SoundSource copySoundSource(){
        return new SoundSource(this.azimuth, this.elevation);
    }*/

    /*public static SoundSource[] arrayManagement(SoundSource[] array){
        int k = array.length;
        for( int i = 0; i < array.length; i++ ){
            if (array[i] == null){break;}
            if (!array[i].isUpdated()){
                array[i] = null;
                while (k > 0){
                    k--;
                    if(array[k] == null){
                        continue;
                    }else {
                        array[i] = array[k].copySoundSource();
                        array[k] = null;
                        break;
                    }
                }
            }
        }
        return array;
    }*/
}
