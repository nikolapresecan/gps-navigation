package com.example.gpsnavigacija;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

public class PoljeDetailsActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager2;
    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polje_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Polje polje = (Polje) getIntent().getSerializableExtra("polje");
        if (polje == null) {
            Toast.makeText(this, "Nema podataka o polju", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager2 = findViewById(R.id.view_pager);

        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        PoljeDetailsFragment detailFragment = new PoljeDetailsFragment();
                        Bundle args = new Bundle();
                        args.putSerializable("polje", polje);
                        detailFragment.setArguments(args);
                        return detailFragment;
                    case 1:
                        PoljeEditFragment editFragment = new PoljeEditFragment();
                        Bundle argsEdit = new Bundle();
                        argsEdit.putSerializable("polje", polje);
                        editFragment.setArguments(argsEdit);
                        return editFragment;
                    default:
                        return new PoljeDetailsFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.getTabAt(position).select();
            }
        });
    }
}