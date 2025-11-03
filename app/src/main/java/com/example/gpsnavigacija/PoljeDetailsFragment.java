package com.example.gpsnavigacija;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoljeDetailsFragment extends Fragment {

    private static final String TAG = "PoljeDetailsFragment";
    private Polje polje;
    private TextView tvNazivPolja, tvPovrsinaPolja, tvLokacijaPolja;
    private TableLayout tableAktivnosti;
    private FirebaseConnection firebaseConnection;
    private Map<String, String> tipoviObradeMap = new HashMap<>();
    private Button btnPrikazi;
    private Tocka centerTocka;

    private List<Tocka> fieldPoints = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_polje_details, container, false);

        tvNazivPolja = view.findViewById(R.id.tvNazivPolja);
        tvPovrsinaPolja = view.findViewById(R.id.tvPovrsinaPolja);
        tableAktivnosti = view.findViewById(R.id.tableAktivnosti);
        tvLokacijaPolja = view.findViewById(R.id.tvLokacijaPolja);
        btnPrikazi = view.findViewById(R.id.btnPrikaziNaMapi);

        firebaseConnection = new FirebaseConnection();

        Bundle args = getArguments();
        if (args != null) {
            polje = (Polje) args.getSerializable("polje");
        }
        if (polje == null) {
            Toast.makeText(getContext(), "Nema podataka o polju", Toast.LENGTH_SHORT).show();
            return view;
        }

        tvNazivPolja.setText(polje.getNaziv());
        tvPovrsinaPolja.setText(String.format("Površina: %.2f ha", polje.getPovrsina()));

        firebaseConnection.fetchWorkType(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tipoviObradeMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String tipId = ds.getKey();
                    TipObrade tip = ds.getValue(TipObrade.class);
                    if (tipId != null && tip != null) {
                        tipoviObradeMap.put(tipId, tip.getNaziv());
                    }
                }

                fetchAktivnosti();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Greška pri dohvaćanju tipova obrade", Toast.LENGTH_SHORT).show();

            }
        });

        firebaseConnection.fetchFieldPoints(polje.getId(), new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Tocka> tocke = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Tocka tocka = ds.getValue(Tocka.class);
                    if (tocka != null) {
                        tocke.add(tocka);
                    }
                }
                if (!tocke.isEmpty()) {
                    fieldPoints = new ArrayList<>(tocke);
                    centerTocka = calculateCenter(tocke);
                    tvLokacijaPolja.setText(String.format("Lokacija: %.6f, %.6f", centerTocka.getLatitude(), centerTocka.getLongitude()));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Greška pri dohvaćanju točaka", Toast.LENGTH_SHORT).show();
            }
        });

        btnPrikazi.setOnClickListener(v -> {
            if (centerTocka != null) {
                Intent intent = new Intent(getActivity(), HomeActivity.class);
                intent.putExtra("center_lat", centerTocka.getLatitude());
                intent.putExtra("center_lng", centerTocka.getLongitude());
                intent.putExtra("field_points", (Serializable) fieldPoints);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Središnja lokacija nije dostupna", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void fetchAktivnosti() {
        firebaseConnection.fetchActivityForField(polje.getId(), new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tableAktivnosti.removeAllViews();

                TableRow headerRow = new TableRow(getContext());
                headerRow.setGravity(Gravity.CENTER);
                TextView tvHeaderDatum = new TextView(getContext());
                tvHeaderDatum.setText("Datum");
                tvHeaderDatum.setPadding(8, 8, 8, 8);
                tvHeaderDatum.setGravity(Gravity.CENTER);
                TextView tvHeaderTip = new TextView(getContext());
                tvHeaderTip.setText("Tip obrade");
                tvHeaderTip.setPadding(8, 8, 8, 8);
                tvHeaderTip.setGravity(Gravity.CENTER);
                headerRow.addView(tvHeaderDatum);
                headerRow.addView(tvHeaderTip);
                tableAktivnosti.addView(headerRow);

                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Aktivnost aktivnost = ds.getValue(Aktivnost.class);
                    if (aktivnost != null) {
                        TableRow row = new TableRow(getContext());
                        row.setGravity(Gravity.CENTER);
                        TextView tvDatum = new TextView(getContext());
                        tvDatum.setText(aktivnost.getDatum());
                        tvDatum.setPadding(8, 8, 8, 8);
                        tvDatum.setGravity(Gravity.CENTER);
                        TextView tvTip = new TextView(getContext());
                        String tipNaziv = tipoviObradeMap.get(aktivnost.getId_tipa_obrade());
                        if (tipNaziv == null) {
                            tipNaziv = "Nepoznato";
                        }
                        tvTip.setText(tipNaziv);
                        tvTip.setPadding(8, 8, 8, 8);
                        tvTip.setGravity(Gravity.CENTER);
                        row.addView(tvDatum);
                        row.addView(tvTip);
                        tableAktivnosti.addView(row);
                        count++;
                    }
                }

                if (count == 0) {
                    Toast.makeText(getContext(), "Nema aktivnosti za ovo polje", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Greška pri dohvaćanju aktivnosti", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Tocka calculateCenter(List<Tocka> tocke) {
        double sumLatitude = 0.0;
        double sumLongitude = 0.0;

        for (Tocka tocka : tocke) {
            sumLatitude += tocka.getLatitude();
            sumLongitude += tocka.getLongitude();
        }

        int numPoints = tocke.size();
        return new Tocka(sumLatitude / numPoints, sumLongitude / numPoints);
    }
}