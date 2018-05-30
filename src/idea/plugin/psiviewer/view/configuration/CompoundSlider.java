/*
    IDEA Plugin
    Copyright (C) 2002 Andrew J. Armstrong

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

	Author:
	Andrew J. Armstrong <andrew_armstrong@bigpond.com>
*/

package idea.plugin.psiviewer.view.configuration;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.ColorSelectionModel;
import java.awt.*;

class CompoundSlider extends JPanel
{
    private final JSlider _slider = new JSlider(0, 255, 255);
    private final JSpinner _spinner = new JSpinner(new SpinnerNumberModel(255, 0, 255, 1));
    private final ColorSelectionModel _model;

    public CompoundSlider(String name, ColorSelectionModel model)
    {
        _model = model;
        _slider.setMajorTickSpacing(85);
        _slider.setMinorTickSpacing(17);
        _slider.setPaintLabels(true);
        _slider.setPaintTicks(true);

        _spinner.addChangeListener(e -> {
                    int value = ((Number) (((JSpinner) e.getSource()).getValue())).intValue();
                    _slider.setValue(value);
                    updateColor(value);
        }
        );
        _slider.addChangeListener(e -> {
                    int value = ((JSlider) e.getSource()).getValue();
                    _spinner.setValue(new Integer(value));
                    updateColor(value);
        }
        );

        setBorder(new EmptyBorder(0, 0, 0, 0));
        add(new JLabel(name));
        add(_slider);
        add(_spinner);
    }

    private void updateColor(int alpha)
    {
        Color color = _model.getSelectedColor();
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        _model.setSelectedColor(color);
    }

    public void setValue(int value)
    {
        _slider.setValue(value);
    }

    public int getValue()
    {
        return _slider.getValue();
    }

}
