package com.example.fridaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    RequestQueue requestQueue;
    String nombreTienda;
    String nombreRepresentante;
    String rfc;
    String direccion;
    String cp;
    String ciudad;
    String estado;
    String celular;
    String regimen;
    String macBarra;
    String macCaja;
    String macCocina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnlog = findViewById(R.id.loginButton);
        requestQueue = Volley.newRequestQueue(this);
        search(); // Llama a la función search para obtener las MACs de las impresoras

        btnlog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText usernameEditText = findViewById(R.id.usernameEditText);
                EditText passwordEditText = findViewById(R.id.passwordEditText);

                // Obtener los valores ingresados
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                // Validar que los campos no estén vacíos
                if (username.isEmpty() || password.isEmpty()) {
                    showToast("Por favor, complete todos los campos.");
                    return;
                }
                String hashedPassword = md5(password + "Hola");
                loginUser(username, hashedPassword);

            }
        });
    }

    public void showToast(String c) {
        Toast.makeText(this, c, Toast.LENGTH_SHORT).show();
    }

    private void log(String user, String rol) {
        Intent principalIntent = new Intent(this, Principal.class);
        principalIntent.putExtra("userId", user);
        principalIntent.putExtra("nombreTienda", nombreTienda);
        principalIntent.putExtra("nombreRepresentante", nombreRepresentante);
        principalIntent.putExtra("rfc", rfc);
        principalIntent.putExtra("direccion", direccion);
        principalIntent.putExtra("cp", cp);
        principalIntent.putExtra("ciudad", ciudad);
        principalIntent.putExtra("estado", estado);
        principalIntent.putExtra("celular", celular);
        principalIntent.putExtra("regimen", regimen);
        principalIntent.putExtra("rol", rol);
        principalIntent.putExtra("macBarra", macBarra);
        principalIntent.putExtra("macCocina", macCocina);
        principalIntent.putExtra("macCaja", macCaja);
        startActivity(principalIntent);
    }

    private String md5(String input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            byte[] result = mDigest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loginUser(String user, String pass) {
        getFiscInfo(user);
        String URL1 = "https://grupocoronador.com/Frida/login.php?id=" + user + "&pass=" + pass;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String user = jsonObject.optString("id");
                                String rol = jsonObject.optString("rol");
                                log(user, rol);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("ERROR TRAER ARTICULOS BD");
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);

    }

    private void search() {
        String URL1 = "https://grupocoronador.com/Frida/fetchPrinters.php";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String nombre = jsonObject.optString("nombre");
                                String mac = jsonObject.optString("mac");

                                if (nombre.equalsIgnoreCase("barra")) {
                                    macBarra = mac;
                                } else if (nombre.equalsIgnoreCase("cocina")) {
                                    macCocina = mac;
                                }
                                else if (nombre.equalsIgnoreCase("caja")) {
                                    macCaja = mac;
                                }


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
            }
        });

        // Agregar la solicitud a la cola
        requestQueue.add(jsonArrayRequest);
    }

    public void getFiscInfo(String username) {
        String URL1 = "https://grupocoronador.com/Frida/fetchDataDouble.php?id="+username;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject jsonObject = response.getJSONObject(i);
                                nombreTienda = jsonObject.optString("nombreTienda");
                                nombreRepresentante = jsonObject.optString("nombreRepresentante");
                                rfc = jsonObject.optString("rfc");
                                direccion = jsonObject.optString("direccion");
                                cp = jsonObject.optString("cp");
                                ciudad = jsonObject.optString("ciudad");
                                estado = jsonObject.optString("estado");
                                celular = jsonObject.optString("celular");
                                regimen = jsonObject.optString("regimen");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showToast("No Connect Fisco");
            }
        });
        requestQueue.add(jsonArrayRequest);
    }
}
