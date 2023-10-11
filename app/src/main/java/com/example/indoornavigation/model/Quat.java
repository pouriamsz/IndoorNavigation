package com.example.indoornavigation.model;

import com.google.ar.sceneform.math.Vector3;

public class Quat {
    com.google.ar.sceneform.math.Quaternion quaternion;

    public Quat(com.google.ar.sceneform.math.Quaternion q){
        this.quaternion = q;
    }

    public com.google.ar.sceneform.math.Quaternion getQuaternion() {
        return quaternion;
    }

    public Vector3 normal(){
        Vector3 eu = this.quaternion2Euler();
        double x = Math.cos(eu.x)*Math.cos(eu.y);
        double y = Math.sin(eu.x)*Math.sin(eu.y);
        double z = Math.sin(eu.y);

        return new Vector3((float)x, (float)y, (float)z);
    }

    public Vector3 quaternion2Euler(){
        Quat r = this.unit();
        double phi = Math.atan2(2*(r.quaternion.w*r.quaternion.x+r.quaternion.y*r.quaternion.z),
                1-2*(r.quaternion.x*r.quaternion.x+r.quaternion.y*r.quaternion.y));
        double theta = Math.asin(2 * (r.quaternion.w*r.quaternion.y - r.quaternion.z*r.quaternion.x));
        double psi = Math.atan2(2*(r.quaternion.x*r.quaternion.y+r.quaternion.w*r.quaternion.z),
                1-2*(r.quaternion.y*r.quaternion.y+r.quaternion.z*r.quaternion.z));

        return new Vector3((float)phi, (float)theta, (float)psi);
    }

    public Quat unit(){
        double k = this.norm();
        Quat qc = new Quat(this.quaternion);
        qc.quaternion.w /= k;
        qc.quaternion.x /= k;
        qc.quaternion.y /= k;
        qc.quaternion.z /= k;

        return qc;
    }

    public double norm(){
        return Math.sqrt(this.norm2());
    }

    public double norm2(){
        return this.quaternion.w*this.quaternion.w +
                this.quaternion.x*this.quaternion.x +
                this.quaternion.y*this.quaternion.y +
                this.quaternion.z*this.quaternion.z;

    }
}
