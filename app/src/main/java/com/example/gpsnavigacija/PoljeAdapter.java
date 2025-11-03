package com.example.gpsnavigacija;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PoljeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Polje> polja;
    private PoljeClickCallback click;
    private PoljeLongClickCallback longClick;

    public PoljeAdapter(List<Polje> polja, PoljeClickCallback click, PoljeLongClickCallback longClick) {
        this.polja = polja;
        this.click = click;
        this.longClick = longClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View poljeView = LayoutInflater.from(parent.getContext()).inflate(R.layout.polje_layout, parent, false);
        return new PoljeViewHolder(poljeView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (polja == null || polja.isEmpty()) {
            return;
        }

        if (holder instanceof PoljeViewHolder){
            PoljeViewHolder poljeViewHolder = (PoljeViewHolder) holder;
            Polje polje = polja.get(position);
            poljeViewHolder.textNaziv.setText(polje.getNaziv());
            poljeViewHolder.textPovrsina.setText(String.format("Površina: %.2f ha", polje.getPovrsina()));
            holder.itemView.setOnClickListener(view -> {
                if (click != null){
                    click.OnClick(polje);
                }
            });
            holder.itemView.setOnLongClickListener(view -> {
                if (longClick != null){
                    longClick.OnLongClickDelete(polje, position);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return polja == null ? 0 : polja.size();
    }

    private class PoljeViewHolder extends RecyclerView.ViewHolder{
        TextView textNaziv, textPovrsina;
        PoljeViewHolder(View view) {
            super(view);
            textNaziv = view.findViewById(R.id.textNaziv);
            textPovrsina = view.findViewById(R.id.textPovrsina);
        }
    }
}
