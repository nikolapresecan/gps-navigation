package com.example.gpsnavigacija;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AktivnostActivity extends AppCompatActivity {

    private Spinner spinnerPolja, spinnerTipObrade;
    private RadioGroup radioGroupSirina;
    private Button btnNewAktivnost;
    private TextView tvNewBranch;
    private FirebaseConnection firebaseConnection;

    private List<Polje> poljaList = new ArrayList<>();
    private List<String> poljaNames = new ArrayList<>();
    private ArrayAdapter<String> adapterPolja;

    private List<TipObrade> tipoviList = new ArrayList<>();
    private List<String> tipoviNames = new ArrayList<>();
    private ArrayAdapter<String> adapterTipovi;

    private List<SirinaGrana> sirineList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aktivnost);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Aktivnosti");
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0F5C10")));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerPolja = findViewById(R.id.spinnerPolja);
        spinnerTipObrade = findViewById(R.id.spinnerTipObrade);
        radioGroupSirina = findViewById(R.id.radioGroupSirinaGrana);
        btnNewAktivnost = findViewById(R.id.btnNewAktivnost);
        tvNewBranch = findViewById(R.id.tvNewBranch);

        firebaseConnection = new FirebaseConnection();

        adapterPolja = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, poljaNames);
        adapterPolja.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPolja.setAdapter(adapterPolja);

        adapterTipovi = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipoviNames);
        adapterTipovi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipObrade.setAdapter(adapterTipovi);

        fetchPoljaForUser();
        fetchTipoviObrade();
        fetchSirineGrana();

        tvNewBranch.setOnClickListener(view -> showAddNewSirinaDialog());

        btnNewAktivnost.setOnClickListener(view -> saveNovaAktivnost());
    }

    private void fetchPoljaForUser() {
        String userId = firebaseConnection.getCurrentUser().getUid();
        firebaseConnection.fetchUserFields(this, poljaList, userId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                poljaNames.clear();
                for (Polje p : poljaList) {
                    poljaNames.add(p.getNaziv());
                }
                adapterPolja.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AktivnostActivity.this, "Greška pri dohvaćanju polja", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTipoviObrade() {
        firebaseConnection.fetchWorkType(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tipoviList.clear();
                tipoviNames.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    TipObrade tip = ds.getValue(TipObrade.class);
                    if (tip != null) {
                        tip.setId(ds.getKey());
                        tipoviList.add(tip);
                        tipoviNames.add(tip.getNaziv());
                    }
                }
                adapterTipovi.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AktivnostActivity.this, "Greška pri dohvaćanju tipova obrade", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSirineGrana() {
        firebaseConnection.fetchBranchWidth(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                sirineList.clear();
                radioGroupSirina.removeAllViews();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    SirinaGrana sg = ds.getValue(SirinaGrana.class);
                    if (sg != null) {
                        sirineList.add(sg);
                        RadioButton rb = new RadioButton(AktivnostActivity.this);
                        rb.setText(String.valueOf(sg.getSirina()) + " m");
                        rb.setTag(ds.getKey());
                        radioGroupSirina.addView(rb);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AktivnostActivity.this, "Greška pri dohvaćanju širina grana", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddNewSirinaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodaj novu širinu");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Spremi", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(AktivnostActivity.this, "Unesite vrijednost", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int sirina = Integer.parseInt(value);
                boolean exists = false;
                for (SirinaGrana sg : sirineList) {
                    if (sg.getSirina() == sirina) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    Toast.makeText(AktivnostActivity.this, "Širina već postoji", Toast.LENGTH_SHORT).show();
                    return;
                }

                SirinaGrana novaSirina = new SirinaGrana(sirina);
                firebaseConnection.addBranchWidth(novaSirina, new SaveCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(AktivnostActivity.this, "Nova širina dodana!", Toast.LENGTH_SHORT).show();
                        fetchSirineGrana();
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(AktivnostActivity.this, "Greška: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (NumberFormatException e) {
                Toast.makeText(AktivnostActivity.this, "Vrijednost mora biti broj", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Odustani", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveNovaAktivnost() {
        int poljePosition = spinnerPolja.getSelectedItemPosition();
        int tipPosition = spinnerTipObrade.getSelectedItemPosition();

        if (poljePosition < 0 || tipPosition < 0) {
            Toast.makeText(this, "Nije odabrano polje ili tip obrade", Toast.LENGTH_SHORT).show();
            return;
        }

        Polje odabranoPolje = poljaList.get(poljePosition);
        TipObrade odabraniTip = tipoviList.get(tipPosition);

        int radioId = radioGroupSirina.getCheckedRadioButtonId();
        if (radioId == -1) {
            Toast.makeText(this, "Odaberite širinu obrade", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadio = findViewById(radioId);
        String sirinaObrade = selectedRadio.getText().toString();
        String id_sirine_grana = selectedRadio.getTag() != null ? selectedRadio.getTag().toString() : sirinaObrade;

        String datum = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault()).format(new Date());
        String id_korisnika = firebaseConnection.getCurrentUser().getUid();
        String id_polja = odabranoPolje.getId();
        String id_tipa_obrade = odabraniTip.getId();
        int obradjena_povrsina = 0;

        Aktivnost novaAktivnost = new Aktivnost(datum, id_korisnika, id_polja, id_sirine_grana, id_tipa_obrade, obradjena_povrsina, null);

        firebaseConnection.newActivity(novaAktivnost, new SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AktivnostActivity.this, "Nova aktivnost dodana!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AktivnostActivity.this, "Greška: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}