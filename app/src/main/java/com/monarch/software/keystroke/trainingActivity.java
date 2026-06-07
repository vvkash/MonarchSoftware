package com.monarch.software.keystroke;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class trainingActivity {
    Context con;

    public trainingActivity(Context con) {
        this.con = con;
    }

    public void performTraining() {
        Log.e("Training", "Started");

        DBHelper mydbhelper = new DBHelper(con, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = mydbhelper.getWritableDatabase();

        String[] col = new String[]{
            Database.b1_x, Database.b1_y,
            Database.b2_x, Database.b2_y,
            Database.b3_x, Database.b3_y,
            Database.b4_x, Database.b4_y,
            Database.b5_x, Database.b5_y,
            Database.b6_x, Database.b6_y,
            Database.b1_size, Database.b2_size,
            Database.b3_size, Database.b4_size,
            Database.b5_size, Database.b6_size,
            Database.b1_pressure, Database.b2_pressure,
            Database.b3_pressure, Database.b4_pressure,
            Database.b5_pressure, Database.b6_pressure,
            Database.p1_press, Database.p2_press,
            Database.p3_press, Database.p4_press,
            Database.p5_press, Database.p6_press,
            Database.p1_p2_flight, Database.p2_p3_flight,
            Database.p3_p4_flight, Database.p4_p5_flight,
            Database.p5_p6_flight,
            Database.p1_p2_trigraph, Database.p2_p3_trigraph,
            Database.p3_p4_trigraph, Database.p4_p5_trigraph,
            Database.p1_p2_digraph, Database.p2_p3_digraph,
            Database.p3_p4_digraph, Database.p4_p5_digraph,
            Database.p5_p6_digraph,
            Database.total_time
        };

        // Load all legitimate data into memory (one DB read)
        Cursor curLeg = db.query(Database.TABLE_NAME, col, null, null, null, null, null);
        int countLeg = curLeg.getCount();
        double[][] legitData = new double[countLeg][45];
        curLeg.moveToFirst();
        for (int i = 0; i < countLeg; i++) {
            for (int j = 0; j < 45; j++) {
                legitData[i][j] = Double.parseDouble(curLeg.getString(j));
            }
            curLeg.moveToNext();
        }
        curLeg.close();

        // Load all impostor data into memory (one DB read)
        Cursor curIlleg = db.query(Database.ILLEGITIMATE_TABLE_NAME, col, null, null, null, null, null);
        int countIlleg = curIlleg.getCount();
        double[][] impostorData = new double[countIlleg][45];
        curIlleg.moveToFirst();
        for (int i = 0; i < countIlleg; i++) {
            for (int j = 0; j < 45; j++) {
                impostorData[i][j] = Double.parseDouble(curIlleg.getString(j));
            }
            curIlleg.moveToNext();
        }
        curIlleg.close();

        Log.e("Training", "Loaded " + countLeg + " legitimate, " + countIlleg + " impostor samples");

        // Set up network once
        errorBackPropagation e = new errorBackPropagation(con);
        e.initialiseHiddenLayer();
        e.initialiseInputLayer();
        e.initialiseOutputLayer();
        e.initialiseWeightsAndBias();
        e.initialiseChangeInWeightsAndBias();
        e.initialiseError();
        e.initialiseInputForHiddenLayer();
        e.initialiseInputForOutputLayer();
        e.initialiseTargetOutput();

        // Load existing weights or randomize (one DB read)
        Cursor weightCur = db.rawQuery("SELECT * from " + DatabaseNetwork.NETWORK_TABLE_NAME, null);
        if (weightCur.getCount() != 0) {
            e.getFinalWeightsFromInputToHidden(con);
            e.getFinalWeightsFromHiddenToOutput();
        } else {
            e.randomizeWeights();
        }
        weightCur.close();

        // Train entirely in memory — 50 epochs, zero DB I/O per step
        int EPOCHS = 50;
        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            for (int i = 0; i < countLeg; i++) {
                e.setLegitimatetargetOutput();
                e.getInputLayer(legitData[i]);
                e.functionHiddenLayer();
                e.functionOutputLayer();
                e.functionErrorOutputLayer();
                e.functionChangeInWeightsHiddenToOutput();
                e.functionErrorHiddenLayer();
                e.functionChangeInWeightsInputToHidden();
                e.updateWeights();
            }
            for (int i = 0; i < countIlleg; i++) {
                e.setIllegitimatetargetOutput();
                e.getInputLayer(impostorData[i]);
                e.functionHiddenLayer();
                e.functionOutputLayer();
                e.functionErrorOutputLayer();
                e.functionChangeInWeightsHiddenToOutput();
                e.functionErrorHiddenLayer();
                e.functionChangeInWeightsInputToHidden();
                e.updateWeights();
            }
        }

        // Save weights once at the end (one DB write)
        e.setFinalWeightsFromInputToHidden(con);
        e.setFinalWeightsFromHiddenToOutput();

        Log.e("Training", "Complete");
    }
}
