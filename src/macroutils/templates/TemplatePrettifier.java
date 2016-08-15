package macroutils.templates;

import java.awt.*;
import java.util.*;
import macroutils.*;
import star.base.neo.*;
import star.base.report.*;
import star.common.*;
import star.common.graph.*;
import star.vis.*;

/**
 * Low-level class for prettifying your simulation file with MacroUtils.
 *
 * @since April of 2016
 * @author Fabio Kasper
 */
public class TemplatePrettifier {

    /**
     * Main constructor for this class.
     *
     * @param m given MacroUtils object.
     */
    public TemplatePrettifier(MacroUtils m) {
        _mu = m;
        _sim = m.getSimulation();
    }

    private void _changeAndSayNewName(NamedObject no, String nn, boolean vo) {
        no.setPresentationName(nn);
        _io.say.msg(vo, _FMT2, String.format("New Name: \"%s\".", no.getPresentationName()));
    }

    private boolean _needSkipping(NamedObject no, boolean vo) {
        if (no.getPresentationName().startsWith("#")) {
            _saySkipping(no, vo);
            return true;
        }
        return false;
    }

    private void _sayGeneric(String action, NamedObject no, boolean vo) {
        _io.say.msg(vo, "  - %s \"%s\"...", action, no.getPresentationName());
    }

    private void _sayPrettifying(NamedObject no, boolean vo) {
        _sayGeneric("Prettifying", no, vo);
    }

    private void _saySkipping(NamedObject no, boolean vo) {
        _sayGeneric("Skipping", no, vo);
    }

    private void _setAxes(StarPlot sp) {
        _setLabel(_get.plots.axisX(sp));
        _setLabel(_get.plots.axisY(sp));
    }

    private void _setDatasets(StarPlot sp, ArrayList<DataSet> ads) {
        for (DataSet ds : ads) {
            String dsn = ds.getPresentationName();
            LineStyle ls = ds.getLineStyle();
            SymbolStyle ss = ds.getSymbolStyle();
            LinePatternOption lpo = ls.getLinePatternOption();
            SymbolShapeOption sso = ss.getSymbolShapeOption();
            if (ads.size() == 1 && !_chk.is.residual(sp)) {
                ls.setColor(StaticDeclarations.Colors.SLATE_GRAY.getColor());
            }
            int lw = _LINE_WIDTH;
            if (_chk.is.residual(sp)) {
                if (dsn.equals("Continuity")) {
                    sso.setSelected(SymbolShapeOption.Type.FILLED_SQUARE);
                } else if (dsn.contains("Energy")) {
                    sso.setSelected(SymbolShapeOption.Type.FILLED_SQUARE);
                } else if (dsn.matches("^.dr.*|Tke.*")) {
                    lpo.setSelected(LinePatternOption.Type.DASH);
                    lw += 1;
                }
                if (_chk.has.EMP()) {
                    if (dsn.contains("momentum")) {
                        sso.setSelected(SymbolShapeOption.Type.STAR);
                    } else if (dsn.startsWith("Granular")) {
                        sso.setSelected(SymbolShapeOption.Type.FILLED_DIAMOND);
                    } else if (dsn.contains("-stress")) {
                        sso.setSelected(SymbolShapeOption.Type.STAR);
                    }
                }
            }
            if (ls.getWidth() == 1) {
                ls.setWidth(lw);
            }
            if (ss.getSpacing() == 1) {
                ss.setSpacing(_SYMBOL_SPACING);
            }
            if (ss.getSize() == 6) {
                ss.setSize(_SYMBOL_SIZE);
            }
        }
    }

    private void _setLabel(Cartesian2DAxis axis) {
        axis.getTitle().setFont(StaticDeclarations.Fonts.OTHER.getFont());
        axis.getLabels().setFont(StaticDeclarations.Fonts.OTHER.getFont());
        axis.getLabels().setGridColor(StaticDeclarations.Colors.LIGHT_GRAY.getColor());
        axis.getLabels().getGridLinePatternOption().setSelected(LinePatternOption.Type.DASH);
        axis.getLabels().setGridWidth(_GRID_WIDTH);
        axis.getTicks().setGridVisible(false);
    }

    private void _setLegend(StarPlot sp, ArrayList<DataSet> ads) {
        final DoubleVector defLegPos = new DoubleVector(new double[]{0.85, 0.8});
        MultiColLegend leg = sp.getLegend();
        leg.setFont(StaticDeclarations.Fonts.OTHER.getFont());
        DoubleVector dv = new DoubleVector(new double[]{leg.getRelativeXPosition(), leg.getRelativeYPosition()});
        LegendLayoutOption llo = leg.getLegendLayoutOption();
        ChartPositionOption cpo = leg.getChartPositionOption();
        if (dv.equals(defLegPos) && cpo.getSelectedElement() == ChartPositionOption.Type.CUSTOM) {
            if (ads.size() <= 7) {
                llo.setSelected(LegendLayoutOption.Type.HORIZONTAL);
                cpo.setSelected(ChartPositionOption.Type.SOUTH);
            } else {
                llo.setSelected(LegendLayoutOption.Type.VERTICAL);
                cpo.setSelected(ChartPositionOption.Type.EAST);
            }
        }
    }

    /**
     * Prettifies the simulation file. This method will invoke the other methods:
     * <ul>
     * <li>Prettify {@link #annotations};</li>
     * <li>Prettify {@link #plots};</li>
     * <li>Prettify {@link #scenes};</li>
     * </ul>
     */
    public void all() {
        annotations();
        plots();
        scenes();
    }

    /**
     * Prettifies the Annotations.
     */
    public void annotations() {
        _io.say.action("Prettifying Annotations", true);
        //--
        LogoAnnotation la = (LogoAnnotation) _get.objects.annotation("Logo", false);
        la.setOpacity(0.8);
        _sayPrettifying(la, true);
        //--
        for (star.vis.Annotation a : _sim.getAnnotationManager().getObjects()) {
            boolean isLogo = a instanceof star.vis.LogoAnnotation;
            boolean isPlot = a instanceof star.vis.PlotImage;
            boolean isScn = a instanceof star.vis.SceneAnnotation;
            boolean isSimple = a instanceof star.vis.SimpleAnnotation;
            if (isLogo || isPlot || isScn) {
                continue;
            }
            _sayPrettifying(a, true);
            if (isSimple) {
                a.setFont(StaticDeclarations.Fonts.SIMPLE_ANNOTATIONS.getFont());
                continue;
            }
            a.setFont(StaticDeclarations.Fonts.REPORT_ANNOTATIONS.getFont());
        }
        _io.say.ok(true);
    }

    private void histogram(HistogramPlot hp) {
        HistogramAxisType hat = hp.getXAxisType();
        hat.setNumberOfBin(20);
        String nn = hat.getBinFunction().getFieldFunction().getPresentationName() + " Histogram";
        _changeAndSayNewName(hp, nn, true);
        hp.setTitle(nn);
        hp.setTitleFont(StaticDeclarations.Fonts.TITLE.getFont());
        HistogramDataSet hds = (HistogramDataSet) hp.getDataSetManager().getDataSet(0);
        hds.getFillStyle().setColor(StaticDeclarations.Colors.SLATE_GRAY.getColor());
        hds.getFillStyle().setBackgroundColor(StaticDeclarations.Colors.SLATE_GRAY.getColor());
        _setAxes(hp);
    }

    /**
     * Prettifies the Monitors.
     */
    public void monitors() {
        String s = " Monitor";
        _io.say.action("Prettifying Monitors", true);
        for (Monitor mon : _sim.getMonitorManager().getObjects()) {
            if (_needSkipping(mon, true)) {
                continue;
            }
            _sayPrettifying(mon, true);
            if (mon.getPresentationName().endsWith(s)) {
                String nn = mon.getPresentationName().replace(s, "");
                _changeAndSayNewName(mon, nn, true);
            }
            if (_chk.has.EMP() && _chk.is.residual(mon)) {
                ((ResidualMonitor) mon).getNormalizeOption().setSelected(MonitorNormalizeOption.Type.OFF);
                _io.say.msg(true, _FMT2, "Normalization is now OFF");
            }
        }
        _io.say.ok(true);
    }

    private void plot(StarPlot sp) {
        String name = sp.getPresentationName();
        String[] endsWith = {" Plot", " Monitor"};
        for (String s : endsWith) {
            if (name.endsWith(s)) {
                if (name.contains("-")) {
                    continue;
                }
                String nn = name.replace(s, "");
                _changeAndSayNewName(sp, nn, true);
            }
        }
        ArrayList<DataSet> ad = new ArrayList(sp.getDataSetManager().getDataSets());
        sp.setTitleFont(StaticDeclarations.Fonts.TITLE.getFont());
        _setAxes(sp);
        _setLegend(sp, ad);
        _setDatasets(sp, ad);
    }

    /**
     * Prettifies the Plots.
     */
    public void plots() {
        _io.say.action("Prettifying Plots", true);
        for (StarPlot sp : _sim.getPlotManager().getObjects()) {
            if (_needSkipping(sp, true)) {
                continue;
            }
            _sayPrettifying(sp, true);
            if (_chk.is.histogram(sp)) {
                histogram((HistogramPlot) sp);
                continue;
            }
            plot(sp);
        }
        _io.say.ok(true);
    }

    public void scenes() {
        _io.say.action("Prettifying Scenes", true);
        for (Scene scn : _sim.getSceneManager().getObjects()) {
            if (_needSkipping(scn, true)) {
                continue;
            }
            _sayPrettifying(scn, true);
            _io.say.msg(true, _FMT2, "Setting Solid White Background");
            _set.scene.background(scn, Color.white, false);
            //-- Change Color of Axis
            Color c = StaticDeclarations.Colors.SLATE_GRAY_DARK.getColor();
            scn.setAxesTextColor(new DoubleVector(c.getColorComponents(null)));
        }
        for (Displayer d : _get.scenes.allDisplayers(false)) {
            if (_needSkipping(d, true)) {
                continue;
            }
            _io.say.msg(true, _FMT2, String.format("Displayer: %s", d.getPresentationName()));
            _set.scene.displayerEnhancements(d);
        }
        _io.say.ok(true);
    }

    /**
     * This method is called automatically by {@link MacroUtils}.
     */
    public void updateInstances() {
        _io = _mu.io;
        _chk = _mu.check;
        _get = _mu.get;
        _set = _mu.set;
    }

    //--
    //-- Variables declaration area.
    //--
    final private String _FMT2 = "    - %s...";
    final private int _GRID_WIDTH = 1;
    final private int _LINE_WIDTH = 2;
    final private int _SYMBOL_SIZE = 8;
    final private int _SYMBOL_SPACING = 20;

    private MacroUtils _mu = null;
    private Simulation _sim = null;
    private macroutils.checker.MainChecker _chk = null;
    private macroutils.getter.MainGetter _get = null;
    private macroutils.setter.MainSetter _set = null;
    private macroutils.io.MainIO _io = null;

}