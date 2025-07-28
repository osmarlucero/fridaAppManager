package com.example.fridaapp;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintJob;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import android.content.Context;

public class Principal extends AppCompatActivity {
    private LinearLayout container;
    private DatabaseReference dbRef;
    double tp,tP;
    String cobroCliente="", ticketRepaint="";
    private static final String MY_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";
    private static final UUID MY_UUID = UUID.fromString(MY_UUID_STRING);
    private static final int REQUEST_BLUETOOTH_CONNECT = 1;
    private ArrayList<ArticuloCobro> articulosCobroList = new ArrayList<>(); // ArrayList para los artículos de cobro
    private final Queue<PrintJob> printQueue = new LinkedList<>();
    private boolean isProcessingPrint = false;
    private Map<String, Boolean> comandasImpresas = new HashMap<>(); // Mapa para rastrear comandas impresas
    String mesaPrincipal = " ", mesaSecundaria = " ",tipoUsuario="", macCocina="", macBarra="", macCaja="", idVendor="",nombreTienda,nombreRepresentante,rfc,direccion,cp,ciudad,estado,celular,regimen;
    RequestQueue requestQueue;
    private static final String URLV = "https://grupocoronador.com/Frida/uploadSellV.php";
    private static final String URLM = "https://grupocoronador.com/Frida/uploadMenu.php";
    private static final String URLEM = "https://grupocoronador.com/Frida/updateMenu.php";
    private static final String URLP = "https://grupocoronador.com/Frida/updateTip.php";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        requestQueue = Volley.newRequestQueue(this);
        obtenerArticulosDeServidor();
        // Inicializar Firebase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        dbRef = firebaseDatabase.getReference();
        container = findViewById(R.id.container);
        idVendor = getIntent().getStringExtra("userId");
        nombreTienda = getIntent().getStringExtra("nombreTienda");
        macCocina = getIntent().getStringExtra("macCocina");
        macCaja = getIntent().getStringExtra("macCaja");
        macBarra = getIntent().getStringExtra("macBarra");

        //macCocina = "DC:0D:30:3E:0F:2A";
/*
        macCocina = "DC:0D:51:71:99:D6";
        macCaja = macCocina;
        macBarra = macCocina;
*/
        nombreRepresentante = getIntent().getStringExtra("nombreRepresentante");
        rfc = getIntent().getStringExtra("rfc");
        direccion = getIntent().getStringExtra("direccion");
        cp = getIntent().getStringExtra("cp");
        ciudad = getIntent().getStringExtra("ciudad");
        estado = getIntent().getStringExtra("estado");
        celular = getIntent().getStringExtra("celular");
        regimen = getIntent().getStringExtra("regimen");
        tipoUsuario= getIntent().getStringExtra("rol");
        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Escuchar cambios en la base de datos
        dbRef.child("mesas").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                container.removeAllViews(); // Limpiar las vistas actuales
                if (snapshot.exists()) {
                    // Recorrer cada mesa
                    for (DataSnapshot mesaSnapshot : snapshot.getChildren()) {
                        // Obtener datos de la mesa
                        String mesaId = mesaSnapshot.getKey();
                        String estado = mesaSnapshot.child("estado").getValue(String.class);
                        String cantidadCuenta = mesaSnapshot.child("cantidadCuenta").getValue(String.class);
                        String cobro = mesaSnapshot.child("cobro").getValue(String.class);
                        // Inflar la tarjeta de la mesa
                        LayoutInflater inflater = LayoutInflater.from(Principal.this);
                        LinearLayout mesaCard = (LinearLayout) inflater.inflate(R.layout.card, container, false);
                        // Configurar los textos
                        TextView tvNumeroMesa = mesaCard.findViewById(R.id.tvMesa); // TextView para el número de la mesa
                        TextView tvEstado = mesaCard.findViewById(R.id.tvEstado);
                        // Configurar margen top para la tarjeta
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mesaCard.getLayoutParams();
                        params.setMargins(0, 10, 0, 0);
                        mesaCard.setLayoutParams(params);
                        tvNumeroMesa.setText("Mesa: " + mesaId); // Establecer el número de la mesa
                        tvEstado.setText("Estatus: " + estado);
                        if ("ocupada".equals(estado)) {
                            mesaCard.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#cb658f")));
                        }
                        mesaCard.setOnClickListener(v -> {
                            // Crear un cuadro de diálogo
                            AlertDialog.Builder builder = new AlertDialog.Builder(Principal.this);
                            builder.setTitle("Opciones"); // Título del cuadro de diálogo
                            // Opción 1: Cobrar
                            builder.setPositiveButton("Cobrar", (dialog, which) -> {
                                // Obtener el tipo de cobro de la base de datos y verificar si es "normal"
                                dbRef.child("mesas").child(mesaId).child("cobro").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String tipoCobro = snapshot.getValue(String.class);
                                        if ("normal".equals(tipoCobro)) {
                                            // Cobro normal
                                            cobrarNormal(mesaId);
                                        }else{
                                            // Otro tipo de cobro
                                            cobrarSeparado(mesaId);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        // Manejar el error si ocurre
                                        showToast("Error al obtener el tipo de cobro");
                                    }
                                });
                            });
                            // Opción 2: Agregar Productos
                            builder.setNeutralButton("Ver Mesa", (dialog, which) -> {
                                // Abrir la actividad DetalleMesa y pasar el mesaId como extra
                                Intent intent = new Intent(Principal.this, DetalleMesa.class);
                                intent.putExtra("mesaId", mesaId);
                                intent.putExtra("estado", estado);
                                intent.putExtra("cantidadCuenta", cantidadCuenta);
                                intent.putExtra("cobro", cobro);
                                startActivity(intent);
                            });
                            // Opción 3: Imprimir Ticket
                            builder.setNegativeButton("Imprimir Ticket", (dialog, which) -> {
                                // Obtener el tipo de cobro de la base de datos y verificar si es "normal"
                                dbRef.child("mesas").child(mesaId).child("cobro").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String tipoCobro = snapshot.getValue(String.class);
                                        if ("normal".equals(tipoCobro)) {
                                            // Cobro normal
                                            imprimirNormal(mesaId);
                                        } else {
                                            // Otro tipo de cobro
                                            imprimirSeparado(mesaId);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        // Manejar el error si ocurre
                                        showToast("Error al obtener el tipo de cobro");
                                    }
                                });
                            });

                            // Crear y mostrar el cuadro de diálogo
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        });
                        // Crear una nueva referencia al nodo "comandas" de la mesa actual
                        DatabaseReference comandasRef = mesaSnapshot.child("comandas").getRef();
                        // Agregar ChildEventListener para escuchar los cambios en "comandas"
                        comandasRef.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot comandaSnapshot, @Nullable String previousChildName) {
                                // Verificar si ya se ha impreso esta comanda
                                if (!comandasImpresas.containsKey(comandaSnapshot.getKey())) {
                                    // Obtener el estatus de la comanda
                                    String estatus = comandaSnapshot.child("estatus").getValue(String.class);
                                    if ("no impreso".equals(estatus)) {
                                        // Obtener la lista de bebidas y platillos de la comanda
                                        List<String> bebidas = new ArrayList<>();
                                        List<String> platillos = new ArrayList<>();
                                        // Obtener el nombre del cliente (si existe)
                                        String cliente = comandaSnapshot.child("cliente").getValue(String.class);
                                        // Recorrer todos los hijos de la comanda para clasificarlos como bebidas o platillos
                                        for (DataSnapshot childSnapshot : comandaSnapshot.getChildren()) {
                                            String tipo = childSnapshot.getKey(); // Obtener el tipo (bebida o platillo)
                                            for (DataSnapshot itemSnapshot : childSnapshot.getChildren()) {
                                                int cant = itemSnapshot.child("cantidad").getValue(Integer.class); // Obtener la cantidad del item
                                                String nombre = itemSnapshot.child("nombre").getValue(String.class); // Obtener el nombre del item
                                                String obs = itemSnapshot.child("observaciones").getValue(String.class); // Obtener las observaciones del item
                                                String itemText = cant + " X " + nombre + "\nObservaciones:" + obs+"\n";
                                                if (cliente != null && !cliente.isEmpty()) {
                                                    itemText += " / Cliente: " + cliente;
                                                }
                                                if ("bebidas".equals(tipo)) {
                                                    bebidas.add(itemText); // Si es bebida, agregar al ArrayList de bebidas
                                                } else if ("platillos".equals(tipo)) {
                                                    platillos.add(itemText); // Si es platillo, agregar al ArrayList de platillos
                                                }
                                            }
                                        }
                                        // Mostrar las bebidas en un toast
                                        if (!bebidas.isEmpty()) {
                                            StringBuilder bebidasMessage = new StringBuilder();
                                            bebidasMessage.append("\n\n\n\n")
                                                    .append((char) 0x1B).append((char) 0x21).append((char) 0x10) // Tamaño reducido (posible reducción)
                                                    .append("Mesa ").append(mesaId).append(":\n")
                                                    .append((char) 0x1B).append((char) 0x21).append((char) 0x01); // Tamaño un poco más pequeño
                                            for (String bebida : bebidas) {
                                                bebidasMessage.append((char) 0x1B).append((char) 0x21).append((char) 0x10) // Tamaño reducido (posible reducción)
                                                        .append(bebida).append("\n")
                                                        .append((char) 0x1B).append((char) 0x21).append((char) 0x01); // Tamaño un poco más pequeño
                                            }
                                            makeTicket("bebida", bebidasMessage.toString());
                                        }
                                        // Mostrar los platillos en un ticket
                                        if (!platillos.isEmpty()) {
                                            StringBuilder platillosMessage = new StringBuilder();
                                            platillosMessage.append("\n\n\n\n\n\n\n\n\n\n\n")
                                                    .append((char) 0x1B).append((char) 0x21).append((char) 0x10) // Tamaño reducido (más pequeño que el tamaño grande)
                                                    .append("Mesa ").append(mesaId).append(":\n")
                                                    .append((char) 0x1B).append((char) 0x21).append((char) 0x01); // Tamaño un poco más pequeño
                                            for (String platillo : platillos) {
                                                platillosMessage.append((char) 0x1B).append((char) 0x21).append((char) 0x10) // Tamaño reducido (más pequeño que el tamaño grande)
                                                        .append(platillo).append("\n")
                                                        .append((char) 0x1B).append((char) 0x21).append((char) 0x01); // Tamaño un poco más pequeño
                                            }
                                            makeTicket("comida", platillosMessage.toString());
                                        }

                                        // Cambiar el estatus a "impreso" MOVERANTEDE
                                        comandaSnapshot.getRef().child("estatus").setValue("impreso");
                                        // Agregar la comanda a las comandas impresas
                                        comandasImpresas.put(comandaSnapshot.getKey(), true);
                                    }
                                }
                            }
                            @Override
                            public void onChildChanged(@NonNull DataSnapshot comandaSnapshot, @Nullable String previousChildName) {
                                // No necesitamos implementar este método en este caso
                            }
                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot comandaSnapshot) {
                                // No necesitamos implementar este método en este caso
                            }
                            @Override
                            public void onChildMoved(@NonNull DataSnapshot comandaSnapshot, @Nullable String previousChildName) {
                                // No necesitamos implementar este método en este caso
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Manejar el error si ocurre
                            }
                        });
                        // Añadir la tarjeta al contenedor
                        container.addView(mesaCard);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Mostrar un Toast de error
                Toast.makeText(Principal.this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show();
                Log.e("FirebaseError", "Error al conectar con la base de datos", error.toException());
            }
        });

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu1, menu);
        // Agregar el listener a cada elemento del menú
        MenuItem crearPropina = menu.findItem(R.id.action_propina);
        MenuItem repaint = menu.findItem(R.id.action_repaint);
        MenuItem crearItem = menu.findItem(R.id.action_crear_pedido);
        MenuItem cajasItem = menu.findItem(R.id.action_cajas);
        MenuItem cierreCajaItem = menu.findItem(R.id.action_cierre_caja);
        MenuItem crearX = menu.findItem(R.id.action_generar_x);
        MenuItem crearZ = menu.findItem(R.id.action_generar_z);
        MenuItem crearPedido = menu.findItem(R.id.action_llevar);
        crearPedido.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                crearPedidoLlevar();
                return true;
            }
        });
        repaint.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.e("REPAINT",ticketRepaint);
                makeTicket("caja",ticketRepaint);
                return true;
            }
        });
        crearPropina.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                realizarPropina();
                return true;
            }
        });
        crearItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                moverMesa();
                return true;
            }
        });

        cajasItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                opcionesCaja("abrir");
                return true;
            }
        });
        cierreCajaItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                opcionesCaja("cerrar");
                return true;
            }
        });
        crearZ.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                opcionesCaja("crearZ");
                return true;
            }
        });
        crearX.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getX();
                return true;
            }
        });
        return true;
    }

    private static class PrintJob {
        String type;
        String data;

        PrintJob(String type, String data) {
            this.type = type;
            this.data = data;
        }
    }
    public String getDate() {
        // Obtener la fecha y hora actuales
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        // Mostrar la fecha y hora en un Toast
        return currentDateAndTime;
    }
    public void moverMesa() {
        // Inflar el layout card_cant.xml
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.card_cant, null);
        // Obtener referencia del EditText en card_cant.xml
        EditText etCantidad = view.findViewById(R.id.editTextCantidad);
        // Crear el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mover Mesa")
                .setView(view)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    // Obtener el valor ingresado por el usuario
                    mesaPrincipal = etCantidad.getText().toString();
                    // Mostrar el valor con un Toast
                    moverMesaB(mesaPrincipal);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        // Mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void moverMesaB(String mesa) {
        // Inflar el layout card_cant.xml
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.card_cant, null);
        // Obtener referencia del EditText en card_cant.xml
        EditText etCantidad = view.findViewById(R.id.editTextCantidad);
        // Crear el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mover Mesa")
                .setView(view)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    // Obtener el valor ingresado por el usuario
                    mesaSecundaria = etCantidad.getText().toString();
                    // Mostrar el valor con un Toast
                    moverFinal(mesa,mesaSecundaria);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        // Mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void moverFinal(String mesaUno, String mesaDos) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("mesas");
        // Obtener los datos de mesaUno
        dbRef.child(mesaUno).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Obtener los datos de mesaUno
                    Map<String, Object> mesaUnoData = (Map<String, Object>) snapshot.getValue();
                    if (mesaUnoData != null) {
                        // Copiar los datos de mesaUno a mesaDos
                        dbRef.child(mesaDos).updateChildren(mesaUnoData).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Actualizar el estado, cobro y cantidadCuenta de mesaUno
                                Map<String, Object> updateMesaUno = new HashMap<>();
                                updateMesaUno.put("comandas", null); // Eliminar las comandas
                                updateMesaUno.put("estado", "libre"); // Actualizar el estado a "libre"
                                updateMesaUno.put("cobro", ""); // Vaciar el campo cobro
                                updateMesaUno.put("cantidadCuenta", ""); // Vaciar el campo cantidadCuenta
                                dbRef.child(mesaUno).updateChildren(updateMesaUno).addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        showToast("Mesa movida exitosamente");
                                    } else {
                                        showToast("Error al actualizar mesaUno");
                                    }
                                });
                            } else {
                                showToast("Error al mover los datos a mesaDos");
                            }
                        });
                    }else {
                        showToast("Error al obtener los datos de mesaUno");
                    }
                }else {
                    showToast("La mesaUno no existe");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Error al leer los datos de mesaUno");
            }
        });
    }
    public void showToast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
        Log.e("CHECKER",value);
    }
    public void makeTicket(String type, String data) {
        synchronized (printQueue) {
            printQueue.add(new PrintJob(type, data));
            if (!isProcessingPrint) {
                isProcessingPrint = true;
                processNextPrint();
            }
        }
    }
    private void processNextPrint() {
        new Thread(() -> {
            while (true) {
                PrintJob job;

                synchronized (printQueue) {
                    job = printQueue.poll();
                    if (job == null) {
                        isProcessingPrint = false;
                        return;
                    }
                }

                boolean success = printJob(job.type, job.data);

                if (!success) {
                    runOnUiThread(() -> showToast("Error al imprimir ticket"));
                    // Opcional: puedes re-agregar el trabajo a la cola si quieres reintentar más tarde
                    // synchronized (printQueue) { printQueue.add(job); }
                }

                try {
                    Thread.sleep(300); // pequeña pausa entre tickets
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private boolean printJob(String type, String data) {
        String finalData = data + "\n\n\n";
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return false;
        }

        String PRINTER_ADDRESS;
        if ("bebida".equals(type)) {
            PRINTER_ADDRESS = macBarra;
        } else if ("caja".equals(type)) {
            PRINTER_ADDRESS = macCaja;
            ticketRepaint = finalData;
        } else {
            PRINTER_ADDRESS = macCocina;
        }

        BluetoothDevice printerDevice = bluetoothAdapter.getRemoteDevice(PRINTER_ADDRESS);
        BluetoothSocket bluetoothSocket = null;
        OutputStream outputStream = null;

        try {
            bluetoothSocket = printerDevice.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();

            String[] lineas = finalData.split("\n", -1);
            for (String linea : lineas) {
                while (linea.length() > 40) {
                    String sub = linea.substring(0, 40);
                    outputStream.write((sub + "\n").getBytes("UTF-8"));
                    outputStream.flush();
                    linea = linea.substring(40);
                    Thread.sleep(40);
                }

                outputStream.write((linea + "\n").getBytes("UTF-8"));
                outputStream.flush();
                Thread.sleep(40);
            }

            Thread.sleep(500);
            return true;

        } catch (IOException | InterruptedException e) {
            Log.e("BT", "Error al imprimir", e);
            return false;
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (bluetoothSocket != null) bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("BT", "Error al cerrar socket", e);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_BLUETOOTH_CONNECT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, intenta establecer la conexión Bluetooth
                    makeTicket("",""); // o el tipo de venta que necesites
                } else {
                    // Permiso denegado, muestra un mensaje
                    Toast.makeText(this, "Permiso de Bluetooth denegado", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            // Otros casos de permisos
        }
    }
    public String buildTicketData(String type) {
        StringBuilder ticketBuilder = new StringBuilder();
        ticketBuilder.append("\n\n"+type+"\n\n\n\n");
        Log.e("Ticket",ticketBuilder.toString());
        return ticketBuilder.toString();
    }
    public void obtenerArticulosDeServidor() {
        String URL1 = "https://grupocoronador.com/Frida/fetchMenus.php";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject jsonObject = response.getJSONObject(i);
                                // Crear objeto ArticuloCobro y añadirlo a la lista
                                ArticuloCobro articulo = new ArticuloCobro(
                                        jsonObject.optInt("id"),
                                        jsonObject.optString("nombre"),
                                        1, // Cantidad inicialmente 0
                                        jsonObject.optDouble("precio")
                                );
                                articulosCobroList.add(articulo);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("ERROR TRAER MENU");
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
    }
    public void imprimirNormal(String id) {
        // Crear un ArrayList para almacenar temporalmente los artículos de la comanda
        ArrayList<ArticuloCobro> articulosComanda = new ArrayList<>();
        // Obtener una referencia a la base de datos de Firebase
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        // Obtener las comandas de la base de datos para la mesa especificada
        dbRef.child("mesas").child(id).child("comandas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Limpiar el ArrayList de articulosComanda
                    articulosComanda.clear();

                    for (DataSnapshot comandaSnapshot : snapshot.getChildren()) { // Recorrer todas las comandas
                        for (DataSnapshot itemSnapshot : comandaSnapshot.getChildren()) { // Recorrer los elementos de cada comanda (bebidas y platillos)
                            for (DataSnapshot articuloSnapshot : itemSnapshot.getChildren()) { // Recorrer los artículos de cada tipo
                                if ("cantidad".equals(articuloSnapshot.getKey())) { // Saltar si es el nodo cantidad
                                    continue;
                                }
                                int cantidad = articuloSnapshot.child("cantidad").getValue(Integer.class); // Obtener la cantidad del artículo
                                int itemId = articuloSnapshot.child("id").getValue(Integer.class); // Obtener el ID del artículo
                                boolean encontrado = false; // Variable para verificar si el artículo ya está en la lista
                                // Verificar si el artículo ya está en el ArrayList principal
                                for (ArticuloCobro articulo : articulosComanda) {
                                    if (articulo.getId() == itemId) {
                                        // Se encontró el artículo, se suma la cantidad
                                        articulo.setCantidad(articulo.getCantidad() + cantidad);
                                        articulo.setId(itemId);
                                        encontrado = true;
                                        break;
                                    }
                                }
                                // Si el artículo no está en el ArrayList principal, se agrega
                                if (!encontrado) {
                                    // Buscar el artículo por su ID en la lista de artículos y añadirlo
                                    for (ArticuloCobro articulo : articulosCobroList) {
                                        if (articulo.getId() == itemId) {
                                            articulo.setCantidad(cantidad); // Establecer la cantidad
                                            articulosComanda.add(articulo); // Agregar el artículo al ArrayList temporal
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Imprimir el contenido del ArrayList para verificar los resultados
                StringBuilder ticketDataBuilder = new StringBuilder(); // StringBuilder para construir el string de datos del ticket
                ticketDataBuilder.append((char) 0x1B).append((char) 0x21).append((char) 0x00); // Tamaño normal
                ticketDataBuilder.append("         " + nombreTienda + "\n");
                ticketDataBuilder.append("     " + nombreRepresentante + "\n");
                ticketDataBuilder.append("         " + rfc + "\n");
                ticketDataBuilder.append("     " + direccion + "\n");
                ticketDataBuilder.append("     " + ciudad + " " + estado + "\n");
                ticketDataBuilder.append(" " + regimen + "\n");
                ticketDataBuilder.append(" Fecha: "+getDate()+"\n");
                ticketDataBuilder.append("--------------------------------\n");
                ticketDataBuilder.append("Mesa:"+id+"\n");
                ticketDataBuilder.append(String.format("%-5s %-18s %-8s\n", "Cant", "Producto", "Total"));
                ticketDataBuilder.append("--------------------------------\n");
                double totalPagar = 0, precio = 0;
                for (ArticuloCobro articulo : articulosComanda) {
                    precio = (articulo.getCantidad() * articulo.getPrecio());
                    // Truncar el nombre del artículo si excede los 18 caracteres
                    String nombreArticulo = articulo.getNombre().length() > 18 ? articulo.getNombre().substring(0, 18) : articulo.getNombre();
                    // Construir la línea para el artículo y agregarla al StringBuilder
                    String lineaArticulo = String.format("%-5d %-18s %-8.2f", articulo.getCantidad(), nombreArticulo, precio);
                    ticketDataBuilder.append(lineaArticulo).append("\n");
                    totalPagar += precio;
                }
                ticketDataBuilder.append("--------------------------------\n");
                ticketDataBuilder.append(String.format("%-18s %-8.2f\n", "Total a Pagar:", totalPagar));
                ticketDataBuilder.append((char) 0x1B).append((char) 0x21).append((char) 0x00); // Volver al tamaño normal (por si acaso)
                // Obtener el string completo con los datos del ticket
                String ticketData = ticketDataBuilder.toString();
                // Invocar la función makeTicket con el string de datos del ticket como parámetro
                makeTicket("caja", ticketData);
                precio = 0;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejar el error si ocurre
                Log.e("FirebaseError", "Error al obtener datos de la comanda", error.toException());
            }
        });
    }
    public void cobrarNormal(String id) {
        // Crear un ArrayList para almacenar temporalmente los artículos de la comanda
        ArrayList<ArticuloCobro> articulosComanda = new ArrayList<>();
        // Obtener una referencia a la base de datos de Firebase
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        // Obtener las comandas de la base de datos para la mesa especificada
        dbRef.child("mesas").child(id).child("comandas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Limpiar el ArrayList de articulosComanda
                    articulosComanda.clear();
                    for (DataSnapshot comandaSnapshot : snapshot.getChildren()) { // Recorrer todas las comandas
                        for (DataSnapshot itemSnapshot : comandaSnapshot.getChildren()) { // Recorrer los elementos de cada comanda (bebidas y platillos)
                            for (DataSnapshot articuloSnapshot : itemSnapshot.getChildren()) { // Recorrer los artículos de cada tipo
                                if ("cantidad".equals(articuloSnapshot.getKey())) { // Saltar si es el nodo cantidad
                                    continue;
                                }
                                int cantidad = articuloSnapshot.child("cantidad").getValue(Integer.class); // Obtener la cantidad del artículo
                                int itemId = articuloSnapshot.child("id").getValue(Integer.class); // Obtener el ID del artículo
                                boolean encontrado = false; // Variable para verificar si el artículo ya está en la lista
                                // Verificar si el artículo ya está en el ArrayList principal
                                for (ArticuloCobro articulo : articulosComanda) {
                                    if (articulo.getId() == itemId) {
                                        // Se encontró el artículo, se suma la cantidad
                                        articulo.setCantidad(articulo.getCantidad() + cantidad);
                                        encontrado = true;
                                        break;
                                    }
                                }
                                // Si el artículo no está en el ArrayList principal, se agrega
                                if (!encontrado) {
                                    // Buscar el artículo por su ID en la lista de artículos y añadirlo
                                    for (ArticuloCobro articulo : articulosCobroList) {
                                        if (articulo.getId() == itemId) {
                                            articulo.setCantidad(cantidad); // Establecer la cantidad
                                            articulosComanda.add(articulo); // Agregar el artículo al ArrayList temporal
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } 
                    double totalPagar = 0, precio = 0;
                    for (ArticuloCobro articulo : articulosComanda) {
                        precio = (articulo.getCantidad() * articulo.getPrecio());
                        totalPagar += precio;
                    }
                    // Mostrar el diálogo de pago
                    mostrarDialogoPago(id,totalPagar,articulosComanda,"normal");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejar el error si ocurre
                Log.e("FirebaseError", "Error al obtener datos de la comanda", error.toException());
            }
        });
    }
    private void mostrarDialogoPago(String mesaId, double totalPagar, ArrayList<ArticuloCobro> articulosComanda, String tipoCobro) {
        // Agregar las opciones de pago
        String[] opcionesPago = {"Efectivo", "Debito", "Credito"};
        // Esto es para el carrusel de ventas
        ArrayList<FormaDePago> formasDePago = new ArrayList<>();
        // Inflar el layout card_cobro.xml
        LayoutInflater inflater = getLayoutInflater();
        View cobroView = inflater.inflate(R.layout.card_cobro, null);
        // Obtener referencias a los elementos del layout card_cobro.xml
        Spinner spinnerMetodoPago = cobroView.findViewById(R.id.spinnerMetodoPago);
        EditText editTextCantidadRecibida = cobroView.findViewById(R.id.editTextCantidadRecibida);
        TextView textViewCantidadAPagar = cobroView.findViewById(R.id.textViewCantidadAPagar);
        Button buttonAgregar = cobroView.findViewById(R.id.buttonAgregar);
        Button buttonDescuentoEmpleado = cobroView.findViewById(R.id.buttonDescuentoEmpleado);
        // Crear un ArrayAdapter para el Spinner con las opciones de pago
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesPago);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMetodoPago.setAdapter(adapter);
        textViewCantidadAPagar.setText("Cantidad A Pagar: " + totalPagar);
        tP=totalPagar;
        // Configurar el diálogo de alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(cobroView);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (formasDePago.size() == 0) {
                    showToast("No has agregado ninguna forma de pago");
                } else if (tp > 0) {
                    showToast("Cuenta no cerrada");
                } else {
                    // Calcular la cantidad total recibida
                    double totalRecibido = 0;
                    for (FormaDePago forma : formasDePago) {
                        totalRecibido += forma.getCantidad();
                    }
                    double vuelto = totalRecibido - tP;
                    // Construir el ticket antes de finalizar el cobro
                    StringBuilder ticketDataBuilder = new StringBuilder();
                    ticketDataBuilder.append((char) 0x1B).append((char) 0x21).append((char) 0x00); // Tamaño normal
                    ticketDataBuilder.append("         " + nombreTienda + "\n");
                    ticketDataBuilder.append("     " + nombreRepresentante + "\n");
                    ticketDataBuilder.append("         " + rfc + "\n");
                    ticketDataBuilder.append("     " + direccion + "\n");
                    ticketDataBuilder.append("       " + ciudad + " " + estado + "\n");
                    ticketDataBuilder.append("    " + regimen + "\n");
                    ticketDataBuilder.append(" Fecha: "+getDate()+"\n");
                    ticketDataBuilder.append("--------------------------------\n");
                    ticketDataBuilder.append("Mesa:"+mesaId+"\n");
                    ticketDataBuilder.append("--------------------------------\n");
                    ticketDataBuilder.append(String.format("%-5s %-18s %-8s\n", "Cant", "Producto", "Total"));
                    ticketDataBuilder.append("--------------------------------\n");
                    // Agregar los artículos
                    for (ArticuloCobro articulo : articulosComanda) {
                        ticketDataBuilder.append(String.format("%-5d %-18s %-8.2f\n", articulo.getCantidad(), articulo.getNombre(), articulo.getPrecio() * articulo.getCantidad()));
                    }
                    ticketDataBuilder.append("--------------------------------\n");
                    ticketDataBuilder.append(String.format("TOTAL: %8.2f\n", tP));
                    ticketDataBuilder.append("--------------------------------\n");
                    boolean tarjeta = false;
                    // Agregar las formas de pago
                    for (FormaDePago forma : formasDePago) {
                        ticketDataBuilder.append(String.format("Pago: %-10s %8.2f\n", forma.getNombre(), forma.getCantidad()));
                        if (!forma.getNombre().equals("Efectivo"))
                            tarjeta = true;
                    }
                    // Agregar la cantidad recibida y el vuelto
                    ticketDataBuilder.append(String.format("Recibido: %8.2f\n", totalRecibido));
                    if (tarjeta) {
                        ticketDataBuilder.append(String.format("PROPINA: %8.2f\n", vuelto));
                        //sacarPropina(String.valueOf(vuelto));
                    } else
                        ticketDataBuilder.append(String.format("Vuelto: %8.2f\n", vuelto));

                    // Pasar la cadena generada a la función makeTicket()
                    makeTicket("caja", ticketDataBuilder.toString());
                    Log.e("TICKET", ticketDataBuilder.toString());
                    // Finalizar la mesa
                    finiquitarMesa(mesaId, tP, formasDePago, articulosComanda, tipoCobro);
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Cierra el diálogo
            }
        });

        tp = totalPagar;

        buttonAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextCantidadRecibida.getText().length() != 0) {
                    String metodoPago = spinnerMetodoPago.getSelectedItem().toString(); // Obtener el método de pago seleccionado
                    FormaDePago formaDePago = new FormaDePago(metodoPago, Double.parseDouble(editTextCantidadRecibida.getText().toString()), "");
                    showToast(formaDePago.getNombre() + ":" + formaDePago.getCantidad());
                    formasDePago.add(formaDePago);
                    tp -= formaDePago.getCantidad();
                    textViewCantidadAPagar.setText("Cantidad A Pagar: " + tp);
                    editTextCantidadRecibida.setText("");
                } else {
                    showToast("Ingresa cantidad");
                }
            }
        });

        buttonDescuentoEmpleado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tp = totalPagar / 2;
                tP/=2;
                textViewCantidadAPagar.setText("Cantidad A Pagar: " + tp);
                // Aplicar el descuento a los artículos
                for (ArticuloCobro articulo : articulosComanda) {
                    articulo.setPrecio(articulo.getPrecio() / 2);
                }
            }
        });


        // Mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void finiquitarMesa(String mesaId,double totalPagado,ArrayList<FormaDePago> formasDePago,ArrayList<ArticuloCobro> articulosComanda,String tipoCobro){
        insertData(mesaId,totalPagado,formasDePago,articulosComanda,tipoCobro,getApplicationContext());
    }
    public void insertData(String mesaId, double totalPagado, ArrayList<FormaDePago> formasDePago,
                           ArrayList<ArticuloCobro> articulos, String tipoCobro, Context context) {

        String URLV = "https://grupocoronador.com/Frida/uploadSellV.php"; // URL de la petición

        Gson gson = new Gson();
        String jsonArticulos = gson.toJson(articulos);
        String jsonFormas = gson.toJson(formasDePago);

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, URLV,
                response -> {
                    Log.d("Server Response", response);
                    if ("normal".equals(tipoCobro)) {
                        cerrarMesa(mesaId);
                    } else {
                        cerrarMesaSeparada(mesaId);
                    }
                },
                error -> {
                    String errorMessage = error.toString();
                    showToast(errorMessage);
                    Log.e("Volley Error", errorMessage);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("articulos", jsonArticulos);
                params.put("formas", jsonFormas);
                params.put("caja", idVendor);
                params.put("total", String.valueOf(totalPagado));
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        // Agregar la solicitud a la cola usando MySingleton
        MySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }

    public void cerrarMesa(String mesaId){
        // Obtener la referencia a la base de datos
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mesaRef = database.getReference("mesas").child(mesaId);
        try {
            int mesaIdInt = Integer.parseInt(mesaId);
            if (mesaIdInt > 49) {
                // Si el ID de la mesa es mayor a 49, eliminar todo el nodo
                mesaRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("cerrarMesa", "Mesa con ID mayor a 49 eliminada correctamente.");
                    } else {
                        Log.e("cerrarMesa", "Error al eliminar la mesa con ID mayor a 49.", task.getException());
                    }
                });
            } else {
                // Si el ID de la mesa es menor o igual a 49, eliminar el nodo "comandas" y actualizar los campos
                mesaRef.child("comandas").removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Si la eliminación fue exitosa, actualizar el estado de la mesa a "libre" y el mesero a ""
                        mesaRef.child("estado").setValue("libre").addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                // Actualizar el mesero a ""
                                mesaRef.child("mesero").setValue("").addOnCompleteListener(updateMeseroTask -> {
                                    if (updateMeseroTask.isSuccessful()) {
                                        Log.d("cerrarMesa", "Mesa cerrada correctamente.");
                                    } else {
                                        Log.e("cerrarMesa", "Error al actualizar el mesero de la mesa.", updateMeseroTask.getException());
                                    }
                                });
                            } else {
                                Log.e("cerrarMesa", "Error al actualizar el estado de la mesa.", updateTask.getException());
                            }
                        });
                    } else {
                        Log.e("cerrarMesa", "Error al eliminar las comandas.", task.getException());
                    }
                });
            }
        } catch (NumberFormatException e) {
            Log.e("cerrarMesa", "El ID de la mesa no es un número válido.", e);
        }
    }
    public void cerrarMesaSeparada(String mesaId) {
        showToast(cobroCliente);
        // Obtener la referencia a la base de datos
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mesaRef = database.getReference("mesas").child(mesaId);

        // Obtener las comandas de la mesa especificada
        mesaRef.child("comandas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean comandosEliminadas = false;

                    for (DataSnapshot comandaSnapshot : snapshot.getChildren()) {
                        String cliente = comandaSnapshot.child("cliente").getValue(String.class);
                        if (cliente != null && cliente.equals(cobroCliente)) {
                            // Eliminar la comanda del cliente especificado
                            comandaSnapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("cerrarMesa", "Comanda del cliente " + cobroCliente + " eliminada correctamente.");
                                } else {
                                    Log.e("cerrarMesa", "Error al eliminar la comanda del cliente " + cobroCliente, task.getException());
                                }
                            });
                            comandosEliminadas = true;
                        }
                    }

                    if (comandosEliminadas) {
                        // Volver a verificar si quedan comandas en la mesa
                        mesaRef.child("comandas").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists() || !snapshot.hasChildren()) {
                                    // Si no quedan comandas, actualizar el estado de la mesa a "libre" y el mesero a ""
                                    mesaRef.child("estado").setValue("libre").addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            // Actualizar el mesero a ""
                                            mesaRef.child("mesero").setValue("").addOnCompleteListener(updateMeseroTask -> {
                                                if (updateMeseroTask.isSuccessful()) {
                                                    Log.d("cerrarMesa", "Mesa cerrada correctamente.");
                                                } else {
                                                    Log.e("cerrarMesa", "Error al actualizar el mesero de la mesa.", updateMeseroTask.getException());
                                                }
                                            });
                                        } else {
                                            Log.e("cerrarMesa", "Error al actualizar el estado de la mesa.", updateTask.getException());
                                        }
                                    });
                                } else {
                                    Log.d("cerrarMesa", "Aún quedan comandas en la mesa.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("cerrarMesa", "Error al verificar las comandas restantes.", error.toException());
                            }
                        });
                    } else {
                        Log.d("cerrarMesa", "No se encontraron comandas para el cliente especificado.");
                    }
                } else {
                    Log.d("cerrarMesa", "No existen comandas para la mesa especificada.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("cerrarMesa", "Error al obtener las comandas de la mesa.", error.toException());
            }
        });
    }
    public void opcionesCaja(String tipo) {
        // Crear un LayoutInflater para inflar el layout card_cant.xml
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.card_cant, null);

        // Encontrar el EditText dentro del dialogView
        EditText editTextCantidad = dialogView.findViewById(R.id.editTextCantidad);

        // Crear un AlertDialog.Builder y configurar el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Monto a "+tipo)
                .setView(dialogView)
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Obtener la cantidad ingresada en el EditText
                        String cantidad = editTextCantidad.getText().toString();

                        // Mostrar un Toast con la cantidad ingresada
                        Toast.makeText(Principal.this, "Cantidad ingresada: " + cantidad, Toast.LENGTH_SHORT).show();
                            insertDataCaja(cantidad,tipo);
                    }
                })
                .setNegativeButton("Cancelar", null);

        // Crear y mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void insertDataCaja(String cantidad, String tipo) {
        String URL = "https://grupocoronador.com/Frida/uploadCaja.php";

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(tipo.equals("cerrar"))
                            cerrarCaja(cantidad);
                        else if(tipo.equals("crearZ"))
                            getZ(cantidad);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = error.toString();
                        showToast(errorMessage);
                        Log.e("Volley Error", errorMessage);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("usuario", idVendor);
                params.put("cantidad", cantidad);
                params.put("tipo", tipo);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        // Agregar la solicitud a la cola
        requestQueue.add(stringRequest);
    }
    public void getZ(String cantidad) {
        String URL1 = "https://grupocoronador.com/Frida/getPreZ.php?id=" + idVendor+"&cant="+cantidad;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        double totalEfectivo = 0;
                        double totalCredito = 0;
                        double totalDebito = 0;

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String cantidadRecibida = jsonObject.optString("total");
                                String tipo = jsonObject.optString("tipo");

                                // Calcular el total en efectivo, crédito y débito
                                if ("Efectivo".equals(tipo)) {
                                    totalEfectivo += Double.parseDouble(cantidadRecibida);
                                } else if ("Credito".equals(tipo)) {
                                    totalCredito += Double.parseDouble(cantidadRecibida);
                                } else if ("Debito".equals(tipo)) {
                                    totalDebito += Double.parseDouble(cantidadRecibida);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Calcular el gran total
                        double granTotal = totalEfectivo + totalCredito + totalDebito;

                        // Mostrar los totales en un Toast o procesarlos según tus necesidades
                        String ticket ="\n\nCIERRE DE CAJA (CORTE Z)\n\n"+
                                "         "+nombreTienda+"\n"+
                                "     "+nombreRepresentante+"\n"+
                                "        "+rfc+"\n"+"     "+direccion+"\n"+
                                "     "+ciudad+" "+estado+"\n"+
                                "    "+regimen+"\n"+
                                "  Fecha"+getDate()+"\n"+
                                "Total en Efectivo: " + totalEfectivo + "\n" +
                                "Total en Credito: " + totalCredito + "\n" +
                                "Total en Debito: " + totalDebito + "\n" +
                                "Gran Total: " + granTotal;

                        makeTicket("caja", ticket);
                        //Log.e("TICKET", ticket);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("ERROR CERRAR CAJA");
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
    }
    public void cerrarCaja(String cantidad) {
        String URL1 = "https://grupocoronador.com/Frida/getZ.php?id=" + idVendor;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        double totalEfectivo = 0;
                        double totalCredito = 0;
                        double totalDebito = 0;

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String cantidadRecibida = jsonObject.optString("total");
                                String tipo = jsonObject.optString("tipo");

                                // Calcular el total en efectivo, crédito y débito
                                if ("Efectivo".equals(tipo)) {
                                    totalEfectivo += Double.parseDouble(cantidadRecibida);
                                } else if ("Credito".equals(tipo)) {
                                    totalCredito += Double.parseDouble(cantidadRecibida);
                                } else if ("Debito".equals(tipo)) {
                                    totalDebito += Double.parseDouble(cantidadRecibida);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Calcular el gran total
                        double granTotal = totalEfectivo + totalCredito + totalDebito;

                        // Mostrar los totales en un Toast o procesarlos según tus necesidades
                        String ticket ="\n\nCIERRE DE CAJA (CORTE Z)\n\n"+
                                "         "+nombreTienda+"\n"+
                        "     "+nombreRepresentante+"\n"+
                        "        "+rfc+"\n"+"     "+direccion+"\n"+
                                "     "+ciudad+" "+estado+"\n"+
                        "    "+regimen+"\n"+
                                "  Fecha"+getDate()+"\n"+
                                "Total en Efectivo: " + totalEfectivo + "\n" +
                                "Total en Credito: " + totalCredito + "\n" +
                                "Total en Debito: " + totalDebito + "\n" +
                                "Gran Total: " + granTotal;

                        makeTicket("caja", ticket);
                        //Log.e("TICKET", ticket);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("ERROR CERRAR CAJA");
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
    }

    public void imprimirSeparado(String id){
        // Crear un ArrayList para almacenar temporalmente los artículos de la comanda
        ArrayList<ArticuloCobro> articulosComanda = new ArrayList<>();

            // Crear y mostrar el cuadro de diálogo para ingresar el cliente
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Ingrese el cliente a cobrar");

            // Inflar el layout del cuadro de diálogo
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.card_cant, null);
            builder.setView(dialogView);

            // Obtener la referencia del EditText en el layout inflado
            EditText editTextCantidad = dialogView.findViewById(R.id.editTextCantidad);
            editTextCantidad.setInputType(InputType.TYPE_CLASS_TEXT);
            editTextCantidad.setHint("Introduce Cliente Aqui");
            // Configurar los botones del cuadro de diálogo
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String clienteCobrar = editTextCantidad.getText().toString().trim();
                    if (!clienteCobrar.isEmpty()) {
                        // Obtener una referencia a la base de datos de Firebase
                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                        // Obtener las comandas de la base de datos para la mesa especificada
                        dbRef.child("mesas").child(id).child("comandas").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    // Limpiar el ArrayList de articulosComanda
                                    articulosComanda.clear();

                                    for (DataSnapshot comandaSnapshot : snapshot.getChildren()) { // Recorrer todas las comandas
                                        String cliente = comandaSnapshot.child("cliente").getValue(String.class);
                                        if (cliente != null && cliente.equals(clienteCobrar)) {
                                            for (DataSnapshot itemSnapshot : comandaSnapshot.getChildren()) { // Recorrer los elementos de cada comanda (bebidas y platillos)
                                                for (DataSnapshot articuloSnapshot : itemSnapshot.getChildren()) { // Recorrer los artículos de cada tipo
                                                    if ("cantidad".equals(articuloSnapshot.getKey())) { // Saltar si es el nodo cantidad
                                                        continue;
                                                    }
                                                    int cantidad = articuloSnapshot.child("cantidad").getValue(Integer.class); // Obtener la cantidad del artículo
                                                    int itemId = articuloSnapshot.child("id").getValue(Integer.class); // Obtener el ID del artículo
                                                    String nombre = articuloSnapshot.child("nombre").getValue(String.class); // Obtener el nombre del artículo

                                                    boolean encontrado = false; // Variable para verificar si el artículo ya está en la lista

                                                    // Verificar si el artículo ya está en el ArrayList principal
                                                    for (ArticuloCobro articulo : articulosComanda) {
                                                        if (articulo.getId() == itemId) {
                                                            // Se encontró el artículo, se suma la cantidad
                                                            articulo.setCantidad(articulo.getCantidad() + cantidad);
                                                            encontrado = true;
                                                            break;
                                                        }
                                                    }
                                                    // Si el artículo no está en el ArrayList principal, se agrega
                                                    if (!encontrado) {
                                                        // Buscar el artículo por su ID en la lista de artículos y añadirlo
                                                        for (ArticuloCobro articulo : articulosCobroList) {
                                                            if (articulo.getId() == itemId) {
                                                                articulo.setCantidad(cantidad); // Establecer la cantidad
                                                                articulo.setNombre(nombre); // Establecer el nombre
                                                                articulosComanda.add(articulo); // Agregar el artículo al ArrayList temporal
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Imprimir el contenido del ArrayList para verificar los resultados
                                StringBuilder ticketDataBuilder = new StringBuilder(); // StringBuilder para construir el string de datos del ticket
                                ticketDataBuilder.append("         "+nombreTienda+"\n");
                                ticketDataBuilder.append("     "+nombreRepresentante+"\n");
                                ticketDataBuilder.append("         "+rfc+"\n");
                                ticketDataBuilder.append("     "+direccion+"\n");
                                ticketDataBuilder.append("     "+ciudad+" "+estado+"\n");
                                ticketDataBuilder.append("    "+regimen+"\n");
                                ticketDataBuilder.append("  Fecha:"+getDate()+"\n");
                                ticketDataBuilder.append("--------------------------------\n");
                                ticketDataBuilder.append("Mesa:"+id+"\n");
                                ticketDataBuilder.append("--------------------------------\n");
                                ticketDataBuilder.append(String.format("%-5s %-18s %-8s\n", "Cant", "Producto", "Total"));
                                ticketDataBuilder.append("--------------------------------\n");
                                double totalPagar = 0, precio = 0;
                                for (ArticuloCobro articulo : articulosComanda) {
                                    precio = articulo.getCantidad() * articulo.getPrecio();
                                    // Truncar el nombre del artículo si excede los 18 caracteres
                                    String nombreArticulo = articulo.getNombre().length() > 18 ? articulo.getNombre().substring(0, 18) : articulo.getNombre();
                                    // Construir la línea para el artículo y agregarla al StringBuilder
                                    String lineaArticulo = String.format("%-5d %-18s %-8.2f", articulo.getCantidad(), nombreArticulo, precio);
                                    ticketDataBuilder.append(lineaArticulo).append("\n");
                                    totalPagar += precio;
                                }
                                ticketDataBuilder.append("-------------------------\n");
                                ticketDataBuilder.append(String.format("%-18s %-8.2f\n", "Total a Pagar:", totalPagar));

                                // Obtener el string completo con los datos del ticket
                                String ticketData = ticketDataBuilder.toString();
                                // Invocar la función makeTicket con el string de datos del ticket como parámetro
                                makeTicket("caja", ticketData);
                                precio = 0;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Manejar el error si ocurre
                                Log.e("FirebaseError", "Error al obtener datos de la comanda", error.toException());
                            }
                        });
                    }
                }
            });
            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            // Mostrar el cuadro de diálogo
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    public void cobrarSeparado(String id) {
        // Crear un ArrayList para almacenar temporalmente los artículos de la comanda
        ArrayList<ArticuloCobro> articulosComanda = new ArrayList<>();

        // Crear y mostrar el cuadro de diálogo para ingresar el cliente
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ingrese el cliente a cobrar");

        // Inflar el layout del cuadro de diálogo
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.card_cant, null);
        builder.setView(dialogView);

        // Obtener la referencia del EditText en el layout inflado
        EditText editTextCantidad = dialogView.findViewById(R.id.editTextCantidad);
        editTextCantidad.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextCantidad.setHint("Introduce Cliente Aquí");

        // Configurar los botones del cuadro de diálogo
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String clienteCobrar = editTextCantidad.getText().toString().trim();
                cobroCliente=clienteCobrar;

                if (!clienteCobrar.isEmpty()) {
                    // Obtener una referencia a la base de datos de Firebase
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    // Obtener las comandas de la base de datos para la mesa especificada
                    dbRef.child("mesas").child(id).child("comandas").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Limpiar el ArrayList de articulosComanda
                                articulosComanda.clear();

                                for (DataSnapshot comandaSnapshot : snapshot.getChildren()) { // Recorrer todas las comandas
                                    String cliente = comandaSnapshot.child("cliente").getValue(String.class);
                                    if (cliente != null && cliente.equals(clienteCobrar)) {
                                        for (DataSnapshot itemSnapshot : comandaSnapshot.getChildren()) { // Recorrer los elementos de cada comanda (bebidas y platillos)
                                            for (DataSnapshot articuloSnapshot : itemSnapshot.getChildren()) { // Recorrer los artículos de cada tipo
                                                if ("cantidad".equals(articuloSnapshot.getKey())) { // Saltar si es el nodo cantidad
                                                    continue;
                                                }
                                                int cantidad = articuloSnapshot.child("cantidad").getValue(Integer.class); // Obtener la cantidad del artículo
                                                int itemId = articuloSnapshot.child("id").getValue(Integer.class); // Obtener el ID del artículo
                                                String nombre = articuloSnapshot.child("nombre").getValue(String.class); // Obtener el nombre del artículo

                                                boolean encontrado = false; // Variable para verificar si el artículo ya está en la lista

                                                // Verificar si el artículo ya está en el ArrayList principal
                                                for (ArticuloCobro articulo : articulosComanda) {
                                                    if (articulo.getId() == itemId) {
                                                        // Se encontró el artículo, se suma la cantidad
                                                        articulo.setCantidad(articulo.getCantidad() + cantidad);
                                                        encontrado = true;
                                                        break;
                                                    }
                                                }
                                                // Si el artículo no está en el ArrayList principal, se agrega
                                                if (!encontrado) {
                                                    // Buscar el artículo por su ID en la lista de artículos y añadirlo
                                                    for (ArticuloCobro articulo : articulosCobroList) {
                                                        if (articulo.getId() == itemId) {
                                                            articulo.setCantidad(cantidad); // Establecer la cantidad
                                                            articulo.setNombre(nombre); // Establecer el nombre
                                                            articulosComanda.add(articulo); // Agregar el artículo al ArrayList temporal
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Calcular el total a pagar
                                double totalPagar = 0, precio = 0;
                                for (ArticuloCobro articulo : articulosComanda) {
                                    precio = articulo.getCantidad() * articulo.getPrecio();
                                    totalPagar += precio;
                                }

                                // Mostrar el diálogo de pago
                                mostrarDialogoPago(id, totalPagar, articulosComanda,"separado");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Manejar el error si ocurre
                            Log.e("FirebaseError", "Error al obtener datos de la comanda", error.toException());
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // Mostrar el cuadro de diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void editarMenu() {
        String URL1 = "https://grupocoronador.com/Frida/getItems.php";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Crear arrays para almacenar los IDs y los nombres de los elementos del menú
                        List<String> idsMenu = new ArrayList<>();
                        List<String> nombresMenu = new ArrayList<>();

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String id = jsonObject.optString("id");
                                String nombre = jsonObject.optString("nombre");

                                // Agregar el ID y el nombre a los arrays respectivos
                                idsMenu.add(id);
                                nombresMenu.add(nombre);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Crear un array adapter para el Spinner de nombre
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(Principal.this, android.R.layout.simple_spinner_item, nombresMenu);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                        // Inflar el diseño del diálogo
                        LayoutInflater inflater = LayoutInflater.from(Principal.this);
                        View dialogView = inflater.inflate(R.layout.card_edit_menu, null);

                        // Inicializar el Spinner de nombre en el diseño del diálogo
                        Spinner spinnerNombre = dialogView.findViewById(R.id.spinnerNombre);
                        EditText editTextPrecio = dialogView.findViewById(R.id.editTextPrecio);
                        spinnerNombre.setAdapter(adapter);

                        // Crear el diálogo
                        AlertDialog.Builder builder = new AlertDialog.Builder(Principal.this);
                        builder.setView(dialogView);
                        builder.setTitle("Editar Menú");

                        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Obtener el índice seleccionado en el Spinner
                                int indexNombre = spinnerNombre.getSelectedItemPosition();

                                // Obtener el ID y nombre seleccionados usando el índice
                                String idSeleccionado = idsMenu.get(indexNombre);
                                String nombreSeleccionado = nombresMenu.get(indexNombre);
                                String precioIngresado = editTextPrecio.getText().toString();
                                updateMenu(idSeleccionado,precioIngresado,nombreSeleccionado);
                            }
                        });

                        builder.setNegativeButton("Cancelar", null);

                        // Mostrar el diálogo
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("ERROR AL OBTENER DATOS DEL SERVIDOR");
                    }
                }
        );
        // Asegúrate de tener inicializada la cola de peticiones antes de agregar la solicitud
        requestQueue.add(jsonArrayRequest);
    }
    public void updateMenu(String idMenu, String precio,String nombre){
        // Asegúrate de que requestQueue está inicializado
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, URLEM,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Server Response", response);
                        showToast(nombre+" Editado");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = error.toString();
                        showToast(errorMessage);
                        Log.e("Volley Error", errorMessage);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("id", idMenu);
                params.put("precio", precio);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        // Agregar la solicitud a la cola
        requestQueue.add(stringRequest);
    }
    public void realizarPropina(){
        // Crear un LayoutInflater para inflar el layout card_cant.xml
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.card_cant, null);
        // Encontrar el EditText dentro del dialogView
        EditText editTextCantidad = dialogView.findViewById(R.id.editTextCantidad);
        editTextCantidad.setHint("Propina a retirar");
        // Crear un AlertDialog.Builder y configurar el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Monto a Retirar")
                .setView(dialogView)
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Obtener la cantidad ingresada en el EditText
                        String cantidad = editTextCantidad.getText().toString();
                        sacarPropina(cantidad);
                    }
                })
                .setNegativeButton("Cancelar", null);

        // Crear y mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void sacarPropina(String cantidad){
        // Asegúrate de que requestQueue está inicializado
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, URLP,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Server Response", response);
                        showToast("Propina Retirada");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = error.toString();
                        showToast(errorMessage);
                        Log.e("Volley Error", errorMessage);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("id", idVendor);
                params.put("cantidad", cantidad);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        // Agregar la solicitud a la cola
        requestQueue.add(stringRequest);
    }
    public void getX() {
        String URL1 = "https://grupocoronador.com/Frida/getX.php?id=" + idVendor;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        double totalEfectivo = 0;
                        double totalCredito = 0;
                        double totalDebito = 0;
                        StringBuilder transaccionesBuilder = new StringBuilder();

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String cantidadRecibida = jsonObject.optString("total");
                                String tipo = jsonObject.optString("tipo");

                                // Añadir la transacción individual al StringBuilder
                            //    transaccionesBuilder.append("\n\nCantidad Recibida: ").append(cantidadRecibida)
                              //          .append("\nTipo: ").append(tipo).append("\n");

                                // Calcular el total en efectivo, crédito y débito
                                if ("Efectivo".equals(tipo)) {
                                    totalEfectivo += Double.parseDouble(cantidadRecibida);
                                } else if ("Credito".equals(tipo)) {
                                    totalCredito += Double.parseDouble(cantidadRecibida);
                                } else if ("Debito".equals(tipo)) {
                                    totalDebito += Double.parseDouble(cantidadRecibida);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Calcular el gran total
                        double granTotal = totalEfectivo + totalCredito + totalDebito;

                        // Crear el string del ticket con los totales y las transacciones individuales
                        String ticket = transaccionesBuilder.toString() + "\n\nARQUEO DE CAJA (CORTE X)\n\n"+
                                "         "+nombreTienda+"\n"+
                                "     "+nombreRepresentante+"\n"+
                                "        "+rfc+"\n"+"     "+direccion+"\n"+
                                "     "+ciudad+" "+estado+"\n"+
                                "    "+regimen+"\n"+
                                "  Fecha"+getDate()+"\n"+
                                "Total en Efectivo: " + totalEfectivo + "\n" +
                                "Total en Credito: " + totalCredito + "\n" +
                                "Total en Debito: " + totalDebito + "\n" +
                                "Gran Total: " + granTotal;

                        makeTicket("caja", ticket);
                        //Log.e("TICKET", ticket);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("ERROR TRAER CIERRE");
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
    }
    public void crearPedidoLlevar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmación");
        builder.setMessage("¿Deseas crear un pedido para llevar?");
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Obtener la referencia al nodo mesas en Firebase
                DatabaseReference mesasRef = FirebaseDatabase.getInstance().getReference("mesas");

                // Obtener el último hijo de mesas
                mesasRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        long lastId = 0;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.getKey() != null) {
                                try {
                                    long currentId = Long.parseLong(snapshot.getKey());
                                    if (currentId > lastId) {
                                        lastId = currentId;
                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        // Si el último ID es menor a 49, el nuevo ID será 50
                        if(lastId < 49){
                            crearNuevoPedido(mesasRef, 50);
                        }else{
                            // Si el último ID es 49 o mayor, el nuevo ID será lastId + 1
                            crearNuevoPedido(mesasRef, lastId + 1);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Manejar errores de base de datos aquí
                        Log.e("FirebaseError", databaseError.getMessage());
                    }
                });
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
    private void crearNuevoPedido(DatabaseReference mesasRef, long newId) {
        // Crear el nuevo nodo con los valores especificados
        Map<String, Object> nuevoPedido = new HashMap<>();
        nuevoPedido.put("cantidadCuenta", "1");
        nuevoPedido.put("cobro", "normal");
        nuevoPedido.put("estado", "libre");
        nuevoPedido.put("mesero", "");

        // Agregar el nuevo nodo a Firebase
        mesasRef.child(String.valueOf(newId)).setValue(nuevoPedido)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Pedido creado con éxito
                        showToast("Pedido para llevar creado con éxito.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Manejar errores al crear el nuevo nodo
                        Log.e("FirebaseError", e.getMessage());
                    }
                });
    }
}
