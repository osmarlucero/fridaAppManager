package com.example.fridaapp;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DetalleMesa extends AppCompatActivity {

    private LinearLayout mainLayout,subMainLayout,buttonLayout; // Layout donde se agregarán las vistas de card_inner_food
    private RequestQueue requestQueue;
    private ArrayAdapter<String> comidaAdapter;
    private List<String> comidaIds = new ArrayList<>();
    private List<String> comidaTipos = new ArrayList<>();
    Button btnAdd,btnGenerate;
    EditText etCantidad;
    String mesaId, clienteLetra="",estado="",userName="",cantidadCuenta="";
    boolean tipoComanda=true;
    int cantidadArt;
    /***  ARRAY LISTA  **/
    List<Articulo> comandaArray = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_mesa);

        // Referencia al layout principal
        mainLayout = findViewById(R.id.main);
        subMainLayout = findViewById(R.id.mainSub);
        buttonLayout = findViewById(R.id.mainButtons);
        btnAdd = findViewById(R.id.btnAdd);
        btnGenerate = findViewById(R.id.btnSubir);
        // Inicializar la cola de solicitudes
        requestQueue = Volley.newRequestQueue(this);

        // Mostrar el mesaId en un Toast
        mesaId = getIntent().getStringExtra("mesaId");
        estado = getIntent().getStringExtra("estado");
        userName = getIntent().getStringExtra("userName");
        cantidadCuenta = getIntent().getStringExtra("cantidadCuenta");
        if(getIntent().getStringExtra("cobro").equals("separados"))
            tipoComanda = false;
        else
            tipoComanda = true;
        fetchBebidasYPlatillos(mesaId);
        // Mostrar el cuadro de diálogo al iniciar la actividad
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog.Builder builderCant = new AlertDialog.Builder(this);
        if (!estado.equals("ocupada")){
            builder.setTitle("Seleccione el tipo de cuenta")
                    .setPositiveButton("Normal", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Mostrar un Toast indicando que se seleccionó "Normal"
                            tipoComanda = true;
                            cambiarEstado("ocupada",mesaId,"1","normal");
                            showAddFoodDialog();
                        }
                    })
                    .setNeutralButton("Separada", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Mostrar un Toast indicando que se seleccionó "Separada"
                            tipoComanda = false;
                            builderCant.setTitle("Cantidad a separar cuenta")
                                    .setView(R.layout.card_pers) // Layout personalizado con EditText
                                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Obtener el valor del EditText
                                            EditText editText = ((AlertDialog) dialog).findViewById(R.id.editTextCantidad);
                                            if (editText != null) {
                                                String cantidad = editText.getText().toString();
                                                // Llamar a la función para crear los botones con la cantidad especificada
                                                crearBotones(Integer.parseInt(cantidad));
                                                cambiarEstado("ocupada", mesaId,cantidad,"separados");
                                            }
                                        }
                                    })
                                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Aquí maneja el evento de hacer clic en Cancelar
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog alertDialogCant = builderCant.create(); // Crear el AlertDialog
                            alertDialogCant.show(); // Mostrar el AlertDialog
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }else{
            int cant=Integer.parseInt(cantidadCuenta);
            if(cant!=1)
                crearBotones(cant);
        }

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddFoodDialog();
            }
        });
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateComand();
            }
        });
    }
    // Función para crear botones dinámicamente
    private void crearBotones(int cantidad) {
        // Limpiar el layout antes de agregar nuevos botones
        buttonLayout.removeAllViews();
        // Crear y agregar los botones dinámicamente
        for (int i = 0; i < cantidad; i++) {
            // Crear el botón
            Button button = new Button(DetalleMesa.this);
            // Asignar el texto correspondiente a cada botón (a-z)
            button.setText(String.valueOf((char) ('a' + i)));
            // Agregar el botón al layout
            buttonLayout.addView(button);
            // Asignar un OnClickListener al botón
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Mostrar un Toast con la letra del botón
                    clienteLetra=((Button) v).getText().toString();
                }
            });
        }
    }
    public void cambiarEstado(String nuevoEstado,String mesaid, String cantidad,String tipoCobro) {
        DatabaseReference databaseReference;
        databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("mesas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot mesaSnapshot : dataSnapshot.getChildren()) {
                    if (mesaSnapshot != null && mesaSnapshot.getKey().equals(mesaid)) {
                        mesaSnapshot.getRef().child("estado").setValue(nuevoEstado);
                        mesaSnapshot.getRef().child("cantidadCuenta").setValue(cantidad);
                        mesaSnapshot.getRef().child("cobro").setValue(tipoCobro);
                        mesaSnapshot.getRef().child("mesero").setValue(userName);
                        // Salir del bucle una vez que hemos encontrado y actualizado la mesa correspondiente
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Error de base de datos: ", databaseError.toException());
            }
        });
    }
    public void showToast(String string){
        Toast.makeText(this,string,Toast.LENGTH_SHORT).show();
    }
    /*********************/
    private void fetchBebidasYPlatillos(String mesaId) {
        DatabaseReference mesaRef = FirebaseDatabase.getInstance().getReference().child("mesas").child(mesaId);

        mesaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Recorrer los nodos hijos de la mesa para obtener comandas
                    for (DataSnapshot comandaSnapshot : dataSnapshot.child("comandas").getChildren()) {
                        // Declarar una variable que obtenga el ID de la comanda
                        String comandaId = comandaSnapshot.getKey();

                        // Mostrar el estado y el mesero de la mesa
            /*String estado = dataSnapshot.child("estado").getValue(String.class);
            String mesero = dataSnapshot.child("mesero").getValue(String.class);
            Toast.makeText(DetalleMesa.this, "Estado: " + estado + "\nMesero: " + mesero, Toast.LENGTH_SHORT).show();
            */

                        // Recuperar bebidas de la comanda actual
                        for (DataSnapshot bebidaSnapshot : comandaSnapshot.child("bebidas").getChildren()) {
                            String cantidadBebida = String.valueOf(bebidaSnapshot.child("cantidad").getValue(Integer.class));
                            String nombreBebida = bebidaSnapshot.child("nombre").getValue(String.class);
                            int idBebida = bebidaSnapshot.child("id").getValue(Integer.class);
                            String observacionesBebida = bebidaSnapshot.child("observaciones").getValue(String.class);
                            // Mostrar el nombre y detalles de la bebida
                            addFoodToMainLayout(cantidadBebida, nombreBebida, observacionesBebida, true, idBebida,comandaId);
                        }

                        // Recuperar platillos de la comanda actual
                        for (DataSnapshot platilloSnapshot : comandaSnapshot.child("platillos").getChildren()) {
                            String cantidadPlatillo = String.valueOf(platilloSnapshot.child("cantidad").getValue(Integer.class));
                            String nombrePlatillo = platilloSnapshot.child("nombre").getValue(String.class);
                            String observacionesPlatillo = platilloSnapshot.child("observaciones").getValue(String.class);
                            int idBebida = platilloSnapshot.child("id").getValue(Integer.class);

                            // Mostrar el nombre y detalles del platillo
                            addFoodToMainLayout(cantidadPlatillo, nombrePlatillo, observacionesPlatillo, true, idBebida, comandaId);
                        }
                    }
                } else {
                    Toast.makeText(DetalleMesa.this, "Mesa no encontrada en la base de datos", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DetalleMesa.this, "Error al recuperar datos de la base de datos", Toast.LENGTH_SHORT).show();
                Log.e("FirebaseError", "Error al recuperar datos de la base de datos", databaseError.toException());
            }
        });
    }
    /*********************/
    /*********************/
    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Comida");

        // Inflar el diseño del cuadro de diálogo
        final View dialogView = getLayoutInflater().inflate(R.layout.card_add_food, null);
        builder.setView(dialogView);

        // Inicializar el adaptador comidaAdapter
        comidaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);

        // Inicializar el spinner de comida después de inflar el layout
        Spinner spinnerComida = dialogView.findViewById(R.id.spinnerTipoProducto);
        spinnerComida.setAdapter(comidaAdapter);

        // Agregar botones personalizados al diálogo
        builder.setView(dialogView)
                .setPositiveButton("Agregar", null) // Este será un botón personalizado, por lo tanto, establecemos el OnClickListener después
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Obtener el botón "Agregar" del diálogo
        Button btnAgregar = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener el texto de los EditText
                etCantidad = dialogView.findViewById(R.id.etCantidad);
                EditText etAnotaciones = dialogView.findViewById(R.id.etAnotaciones);

                // Verificar si los campos están vacíos
                if (etCantidad.length() == 0) {
                    // Mostrar un mensaje de error indicando que los campos están vacíos
                    Toast.makeText(getApplicationContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                } else {
                    String anotaciones = etAnotaciones.getText().toString();
                    String cantidad = etCantidad.getText().toString();
                    // Si los campos no están vacíos, continuar con la lógica de agregar el artículo

                    // Obtener el ID de la comida seleccionada
                    int selectedPosition = spinnerComida.getSelectedItemPosition();
                    String selectedId = comidaIds.get(selectedPosition);
                    String selectedTipo = comidaTipos.get(selectedPosition);

                    // Obtener los datos de la comida seleccionada
                    String comidaSeleccionada = spinnerComida.getSelectedItem().toString();

                    // Convertir la cantidad a entero
                    int cantidadArt = Integer.parseInt(cantidad);

                    // Crear una nueva vista card_inner_food y añadir datos
                    addFoodToMainLayout(cantidad, comidaSeleccionada, anotaciones, false, Integer.parseInt(selectedId),"");

                    // Crear un nuevo objeto Articulo y agregarlo a comandaArray
                    Articulo articulo = new Articulo(Integer.parseInt(selectedId), comidaSeleccionada, cantidadArt, anotaciones,selectedTipo);
                    comandaArray.add(articulo);

                    // Limpiar los EditText
                    etAnotaciones.setText("");
                    etCantidad.setText("");
                }
            }
        });


        // Llamar al método search después de mostrar el diálogo
        search(getIntent().getStringExtra("mesaId"));
    }

    private void addFoodToMainLayout(String cantidad, String comida, String observaciones, boolean tipo, int id, String comanda) {
        // Inflar el layout card_inner_food
        View foodView = LayoutInflater.from(this).inflate(R.layout.card_inner_food, null);

        // Obtener referencias a las vistas dentro de card_inner_food
        TextView tvPlatillo = foodView.findViewById(R.id.tvPlatillo);
        TextView tvObservaciones = foodView.findViewById(R.id.tvObservaciones);
        Button btnEliminar = foodView.findViewById(R.id.btnEliminar);
        btnEliminar.setId(id);
        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener el padre de la vista actual
                ViewGroup parentView = (ViewGroup) foodView.getParent();

                // Eliminar la vista del padre
                parentView.removeView(foodView);
                if(tipo){
                    new AlertDialog.Builder(DetalleMesa.this)
                            .setMessage("¿Deseas eliminar el platillo?")
                            .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteArt(btnEliminar.getId(), comanda);

                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .show();
                }
                deleteComand(btnEliminar.getId());
            }
        });

        ViewGroup foodContainer = (ViewGroup) foodView;
            //controla si se viene de la bd en firebase o se agrego
            //foodContainer.removeView(btnEliminar);



        // Establecer los datos
        tvPlatillo.setText(cantidad+" X "+comida);
        tvObservaciones.setText("Observaciones:" + observaciones);

        // Agregar card_inner_food al layout principal
        if(!tipo)
            mainLayout.addView(foodView);
        else
            subMainLayout.addView(foodView);
    }
    private void deleteArt(int id, String comandaId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        // Navegar a la comanda específica dentro de la mesa y buscar el producto por ID
        databaseReference.child("mesas").child(mesaId).child("comandas").child(comandaId).child("bebidas")
                .orderByChild("id").equalTo(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                        // Similarmente, buscar y eliminar el producto de platillos si existe
                        databaseReference.child("mesas").child(mesaId).child("comandas").child(comandaId).child("platillos")
                                .orderByChild("id").equalTo(id)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            snapshot.getRef().removeValue();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        showToast("Error al eliminar producto: ");
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                      showToast("Error al eliminar producto: " );
                    }
                });
    }

    /*********************/
    private void deleteComand(int id){
        for (Articulo articulo : comandaArray) {
            // Aquí puedes acceder a cada elemento del ArrayList 'comandaArray' utilizando la variable 'articulo'
            // Por ejemplo, podrías imprimir los detalles de cada artículo así:
            System.out.println("ID: " + articulo.getId());
            System.out.println("Nombre: " + articulo.getNombre());
            System.out.println("Cantidad: " + articulo.getCantidad());
            System.out.println("Descripción: " + articulo.getDescripcion());
            System.out.println("-------------------");

            // Verificar si el ID del artículo coincide con el ID que deseas eliminar
            if (articulo.getId() == id) {
                // Eliminar el artículo de la lista
                comandaArray.remove(articulo);
                System.out.println("Artículo con ID " + id + " eliminado correctamente.");
                break; // Salir del bucle una vez que se elimine el artículo deseado
            }
        }

    }
    /****************
     * aqui se genera la comanda para subirla al firebasex
     * ************/
    private void generateComand() {
        // Verificar si comandaArray contiene elementos
        if (comandaArray.isEmpty()) {
            // Mostrar un mensaje indicando que no hay elementos en la comanda
            Toast.makeText(getApplicationContext(), "No hay elementos en la comanda", Toast.LENGTH_SHORT).show();
        } else {
            // Obtener una referencia a la base de datos Firebase
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

            // Crear una nueva entrada para la comanda de platillos
            DatabaseReference comandaRef = databaseRef.child("mesas").child(mesaId).child("comandas").push();
            String tp;
            // Iterar sobre los elementos de comandaArray y agregar cada platillo a la base de datos
            for (Articulo articulo : comandaArray) {
                if(articulo.getDependency().equals("cocina"))
                    tp="platillos";
                else
                    tp="bebidas";
                // Crear una nueva entrada para el platillo en la comanda
                DatabaseReference platilloRef = comandaRef.child(tp).push();

                // Establecer los valores del platillo en la base de datos
                platilloRef.child("id").setValue(articulo.getId());
                platilloRef.child("nombre").setValue(articulo.getNombre());
                platilloRef.child("cantidad").setValue(articulo.getCantidad());
                platilloRef.child("observaciones").setValue(articulo.getDescripcion());
            }
            comandaRef.child("estatus").setValue("no impreso");
            if (!tipoComanda)
                comandaRef.child("cliente").setValue(clienteLetra);

            // Limpiar comandaArray después de subir los datos
            comandaArray.clear();
            if(tipoComanda)
                finish();
        }
    }
    /****************************/
    private void search(String value) {
        String URL1 = "https://grupocoronador.com/connFix/fetchMenus.php?id=" + value;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL1, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for(int i = 0; i < response.length(); i++){
                            try {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String nombre = jsonObject.optString("nombre");
                                String id = jsonObject.optString("id");
                                String tipo = jsonObject.optString("manufactura");

                                // Agregar el nombre al adaptador del spinner
                                comidaAdapter.add(nombre);

                                // Agregar el ID al array de IDs
                                comidaIds.add(id);
                                comidaTipos.add(tipo);
                                // Mostrar el nombre en un Toast (opcional)
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(DetalleMesa.this, "ERROR", Toast.LENGTH_LONG ).show();
            }
        }
        );

        // Agregar la solicitud a la cola
        requestQueue.add(jsonArrayRequest);
    }
}
