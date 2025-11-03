package com.example.gpsnavigacija;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoljeEditFragment extends Fragment {

    private EditText etNazivPolja, etPovrsinaPolja;
    private TableLayout tableAktivnosti;
    private Button btnSaveChanges;
    private FirebaseConnection firebaseConnection;
    private Map<String, String> tipoviObradeMap = new HashMap<>();
    private Polje polje;
    private List<String> obrisaneAktivnosti = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_polje_edit, container, false);

        etNazivPolja = view.findViewById(R.id.etNazivPolja);
        etPovrsinaPolja = view.findViewById(R.id.etPovrsinaPolja);
        tableAktivnosti = view.findViewById(R.id.tableAktivnosti);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        firebaseConnection = new FirebaseConnection();

        if (getArguments() != null) {
            polje = (Polje) getArguments().getSerializable("polje");
        }

        if (polje == null) {
            Toast.makeText(getContext(), "Podaci o polju nisu dostupni", Toast.LENGTH_SHORT).show();
            return view;
        }

        etNazivPolja.setText(polje.getNaziv() != null ? polje.getNaziv() : "");
        etPovrsinaPolja.setText(String.valueOf(polje.getPovrsina()));

        firebaseConnection.fetchWorkType(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tipoviObradeMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    tipoviObradeMap.put(ds.getKey(), ds.child("naziv").getValue(String.class));
                }
                fetchAktivnosti();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Greška pri dohvaćanju tipova obrade", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveChanges.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void fetchAktivnosti() {
        firebaseConnection.fetchActivityForField(polje.getId(), new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tableAktivnosti.removeAllViews();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String activityId = ds.getKey();
                    Aktivnost aktivnost = ds.getValue(Aktivnost.class);
                    if (aktivnost != null) {
                        addActivityRow(activityId, aktivnost);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Greška pri dohvaćanju aktivnosti", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addActivityRow(String activityId, Aktivnost aktivnost) {
        TableRow row = new TableRow(getContext());
        row.setGravity(Gravity.CENTER);
        row.setTag(activityId);

        TextView tvDatum = new TextView(getContext());
        tvDatum.setText(aktivnost.getDatum());
        tvDatum.setPadding(8, 8, 8, 8);
        tvDatum.setGravity(Gravity.CENTER);

        TextView tvTip = new TextView(getContext());
        String tipNaziv = tipoviObradeMap.get(aktivnost.getId_tipa_obrade());
        tvTip.setText(tipNaziv != null ? tipNaziv : "Nepoznato");
        tvTip.setPadding(8, 8, 8, 8);
        tvTip.setGravity(Gravity.CENTER);

        Button btnDelete = new Button(getContext());
        btnDelete.setText("X");
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0F5C10")));
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setOnClickListener(v -> markActivityForDeletion(row));

        row.addView(tvDatum);
        row.addView(tvTip);
        row.addView(btnDelete);
        tableAktivnosti.addView(row);
    }

    private void markActivityForDeletion(TableRow row) {
        String activityId = (String) row.getTag();
        if (activityId != null) {
            obrisaneAktivnosti.add(activityId);
            tableAktivnosti.removeView(row);
        }
    }

    private void saveChanges() {
        String noviNaziv = etNazivPolja.getText().toString().trim();
        String povrsinaText = etPovrsinaPolja.getText().toString().trim();

        if (noviNaziv.isEmpty() || povrsinaText.isEmpty()) {
            Toast.makeText(getContext(), "Molimo unesite naziv i površinu", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double novaPovrsina = Double.parseDouble(povrsinaText);

            firebaseConnection.updateField(polje.getId(), noviNaziv, novaPovrsina, new UpdateCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Polje ažurirano!", Toast.LENGTH_SHORT).show();
                    polje.setNaziv(noviNaziv);
                    polje.setPovrsina(novaPovrsina);

                    deleteMarkedActivities(() -> {
                        ViewPager2 viewPager2 = getActivity().findViewById(R.id.view_pager);
                        if (viewPager2 != null) {
                            viewPager2.setAdapter(new FragmentStateAdapter(getActivity()) {
                                @Override
                                public Fragment createFragment(int position) {
                                    if (position == 0) {
                                        PoljeDetailsFragment detailsFragment = new PoljeDetailsFragment();
                                        Bundle args = new Bundle();
                                        args.putSerializable("polje", polje);
                                        detailsFragment.setArguments(args);
                                        return detailsFragment;
                                    } else {
                                        PoljeEditFragment editFragment = new PoljeEditFragment();
                                        Bundle args = new Bundle();
                                        args.putSerializable("polje", polje);
                                        editFragment.setArguments(args);
                                        return editFragment;
                                    }
                                }

                                @Override
                                public int getItemCount() {
                                    return 2;
                                }
                            });
                        }

                        if (getActivity() != null) {
                            getActivity().finish();
                            getActivity().startActivity(getActivity().getIntent());
                        }
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), "Greška pri ažuriranju: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Površina mora biti broj!", Toast.LENGTH_SHORT).show();
        }
    }


    private void deleteMarkedActivities(Runnable onComplete) {
        if (obrisaneAktivnosti.isEmpty()) {
            onComplete.run();
            return;
        }

        for (String activityId : obrisaneAktivnosti) {
            firebaseConnection.deleteActivity(activityId, new DeleteCallback() {
                @Override
                public void onSuccess() {
                    obrisaneAktivnosti.remove(activityId);
                    if (obrisaneAktivnosti.isEmpty()) {
                        onComplete.run();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), "Greška pri brisanju: " + errorMessage, Toast.LENGTH_SHORT).show();
                    obrisaneAktivnosti.remove(activityId);
                    if (obrisaneAktivnosti.isEmpty()) {
                        onComplete.run();
                    }
                }
            });
        }
    }

}