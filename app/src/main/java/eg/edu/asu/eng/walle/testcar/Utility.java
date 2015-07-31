package eg.edu.asu.eng.walle.testcar;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

public class Utility {

    public final static String STOP = "S", FORWARD = "F", BACK = "B", LEFT = "L", RIGHT = "R"
            , FORWARD_LEFT = "G", FORWARD_RIGHT = "I", BACK_LEFT = "H", BACK_RIGHT = "J";

    static void sendData(String signal, OutputStream mmOutputStream, Context context) throws IOException
    {
        if (mmOutputStream != null){
            byte[] msgBuffer = signal.getBytes();
            try {
                for ( int i = 0;i < 20;i++)
                    mmOutputStream.write(msgBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(context, "Not Connected", Toast.LENGTH_LONG) ;
            toast.show();
        }
    }
}
