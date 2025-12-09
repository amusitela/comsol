/*
 * COMSOL Java API - Cylinder Flow (Karman Vortex Street)
 * For COMSOL 6.0 Desktop
 * 
 * Features:
 * 1. Reads all parameters from config.json
 * 2. Supports GUI configuration via CylinderFlowGUI
 * 3. Configurable boundary conditions, mesh, solver settings
 * 4. Exports velocity/vorticity images and animation
 */

import java.io.IOException;

import com.comsol.model.*;
import com.comsol.model.util.*;

public class CylinderFlow {

    // Configuration object - loaded from config.json
    private static SimulationConfig config;

    public static void main(String[] args) {
        // Load configuration
        try {
            config = ConfigManager.loadConfig("config.json");
            System.out.println("Configuration loaded:");
            System.out.println(config);
        } catch (IOException e) {
            System.out.println("Warning: Could not load config.json, using defaults");
            config = SimulationConfig.getDefault();
        }
        run();
    }

    public static Model run() {
        // Ensure config is initialized
        if (config == null) {
            config = SimulationConfig.getDefault();
        }

        Model model = ModelUtil.create("Model");
        model.label("Cylinder Flow - Karman Vortex Street");

        // ============================================
        // 1. Geometry: Configurable domain with cylinder hole
        // ============================================
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 2);

        // Rectangle domain: configurable size
        model.component("comp1").geom("geom1").create("r1", "Rectangle");
        model.component("comp1").geom("geom1").feature("r1").set("size",
                new double[] { config.domainWidth, config.domainHeight });
        model.component("comp1").geom("geom1").feature("r1").set("pos", new double[] { 0, 0 });

        // Cylinder (circle): configurable radius and position
        model.component("comp1").geom("geom1").create("c1", "Circle");
        model.component("comp1").geom("geom1").feature("c1").set("r", config.cylinderRadius);
        model.component("comp1").geom("geom1").feature("c1").set("pos",
                new double[] { config.cylinderX, config.cylinderY });

        // Difference: cut cylinder from rectangle
        model.component("comp1").geom("geom1").create("dif1", "Difference");
        model.component("comp1").geom("geom1").feature("dif1").selection("input").set("r1");
        model.component("comp1").geom("geom1").feature("dif1").selection("input2").set("c1");

        // ============================================
        // Named Selections - Using 'inside' for strict isolation
        // ============================================

        // Inlet (x = 0) - dynamically calculated based on geometry
        model.component("comp1").geom("geom1").create("inlet_sel", "BoxSelection");
        model.component("comp1").geom("geom1").feature("inlet_sel").set("entitydim", 1);
        model.component("comp1").geom("geom1").feature("inlet_sel").label("Inlet Selection");
        model.component("comp1").geom("geom1").feature("inlet_sel").set("xmin", -0.05);
        model.component("comp1").geom("geom1").feature("inlet_sel").set("xmax", 0.05);
        model.component("comp1").geom("geom1").feature("inlet_sel").set("ymin", -0.1);
        model.component("comp1").geom("geom1").feature("inlet_sel").set("ymax", config.domainHeight + 0.1);
        model.component("comp1").geom("geom1").feature("inlet_sel").set("condition", "inside");

        // Outlet (x = domainWidth) - dynamically calculated
        model.component("comp1").geom("geom1").create("outlet_sel", "BoxSelection");
        model.component("comp1").geom("geom1").feature("outlet_sel").set("entitydim", 1);
        model.component("comp1").geom("geom1").feature("outlet_sel").label("Outlet Selection");
        model.component("comp1").geom("geom1").feature("outlet_sel").set("xmin", config.domainWidth - 0.05);
        model.component("comp1").geom("geom1").feature("outlet_sel").set("xmax", config.domainWidth + 0.05);
        model.component("comp1").geom("geom1").feature("outlet_sel").set("ymin", -0.1);
        model.component("comp1").geom("geom1").feature("outlet_sel").set("ymax", config.domainHeight + 0.1);
        model.component("comp1").geom("geom1").feature("outlet_sel").set("condition", "inside");

        // Bottom wall (y = 0) - dynamically calculated
        model.component("comp1").geom("geom1").create("bottom_sel", "BoxSelection");
        model.component("comp1").geom("geom1").feature("bottom_sel").set("entitydim", 1);
        model.component("comp1").geom("geom1").feature("bottom_sel").label("Bottom Selection");
        model.component("comp1").geom("geom1").feature("bottom_sel").set("xmin", -0.1);
        model.component("comp1").geom("geom1").feature("bottom_sel").set("xmax", config.domainWidth + 0.1);
        model.component("comp1").geom("geom1").feature("bottom_sel").set("ymin", -0.05);
        model.component("comp1").geom("geom1").feature("bottom_sel").set("ymax", 0.05);
        model.component("comp1").geom("geom1").feature("bottom_sel").set("condition", "inside");

        // Top wall (y = domainHeight) - dynamically calculated
        model.component("comp1").geom("geom1").create("top_sel", "BoxSelection");
        model.component("comp1").geom("geom1").feature("top_sel").set("entitydim", 1);
        model.component("comp1").geom("geom1").feature("top_sel").label("Top Selection");
        model.component("comp1").geom("geom1").feature("top_sel").set("xmin", -0.1);
        model.component("comp1").geom("geom1").feature("top_sel").set("xmax", config.domainWidth + 0.1);
        model.component("comp1").geom("geom1").feature("top_sel").set("ymin", config.domainHeight - 0.05);
        model.component("comp1").geom("geom1").feature("top_sel").set("ymax", config.domainHeight + 0.05);
        model.component("comp1").geom("geom1").feature("top_sel").set("condition", "inside");

        // Cylinder wall (circle boundary) - use BallSelection with configurable
        // position
        model.component("comp1").geom("geom1").create("cyl_sel", "BallSelection");
        model.component("comp1").geom("geom1").feature("cyl_sel").set("entitydim", 1);
        model.component("comp1").geom("geom1").feature("cyl_sel").label("Cylinder Selection");
        model.component("comp1").geom("geom1").feature("cyl_sel").set("r", config.cylinderRadius * 1.6);
        model.component("comp1").geom("geom1").feature("cyl_sel").set("posx", config.cylinderX);
        model.component("comp1").geom("geom1").feature("cyl_sel").set("posy", config.cylinderY);
        model.component("comp1").geom("geom1").feature("cyl_sel").set("condition", "inside");

        // Build geometry
        model.component("comp1").geom("geom1").run();

        // ============================================
        // 2. Material: Configurable fluid properties
        // ============================================
        model.component("comp1").material().create("mat1", "Common");
        model.component("comp1").material("mat1").label(config.fluidName);
        model.component("comp1").material("mat1").selection().all();
        model.component("comp1").material("mat1").propertyGroup("def").set("density",
                config.density + "[kg/m^3]");
        model.component("comp1").material("mat1").propertyGroup("def").set("dynamicviscosity",
                config.dynamicViscosity + "[Pa*s]");

        // ============================================
        // 3. Physics: Configurable Flow Settings
        // ============================================
        model.component("comp1").physics().create("spf", "LaminarFlow", "geom1");
        model.component("comp1").physics("spf").prop("EquationForm").set("form", config.equationForm);

        // Inlet - configurable boundary condition
        model.component("comp1").physics("spf").create("inl1", "InletBoundary", 1);
        model.component("comp1").physics("spf").feature("inl1").selection().named("geom1_inlet_sel");
        model.component("comp1").physics("spf").feature("inl1").set("BoundaryCondition", config.inletType);
        if ("Velocity".equals(config.inletType)) {
            model.component("comp1").physics("spf").feature("inl1").set("U0in", config.inletVelocity + "[m/s]");
        } else {
            model.component("comp1").physics("spf").feature("inl1").set("p0", config.inletPressure);
        }
        model.component("comp1").physics("spf").feature("inl1").label("Inlet");

        // Outlet - configurable boundary condition
        model.component("comp1").physics("spf").create("out1", "OutletBoundary", 1);
        model.component("comp1").physics("spf").feature("out1").selection().named("geom1_outlet_sel");
        model.component("comp1").physics("spf").feature("out1").set("p0", config.outletPressure);
        model.component("comp1").physics("spf").feature("out1").label("Outlet");

        // Top boundary - configurable (Symmetry/Wall/Slip)
        setupBoundary(model, "top", "geom1_top_sel", config.topBoundaryType);

        // Bottom boundary - configurable (Symmetry/Wall/Slip)
        setupBoundary(model, "bottom", "geom1_bottom_sel", config.bottomBoundaryType);

        // Cylinder Wall
        model.component("comp1").physics("spf").create("wall_cyl", "Wall", 1);
        model.component("comp1").physics("spf").feature("wall_cyl").selection().named("geom1_cyl_sel");
        model.component("comp1").physics("spf").feature("wall_cyl").label("Cylinder Wall");

        // ============================================
        // 4. Mesh - Configurable mesh settings
        // ============================================
        model.component("comp1").mesh().create("mesh1");

        // Global size: configurable
        model.component("comp1").mesh("mesh1").feature("size").set("hauto", config.meshSizeLevel);
        model.component("comp1").mesh("mesh1").feature("size").set("custom", "on");
        model.component("comp1").mesh("mesh1").feature("size").set("hmax", String.valueOf(config.meshMaxSize));
        model.component("comp1").mesh("mesh1").feature("size").set("hmin", String.valueOf(config.meshMinSize));

        // Cylinder refinement - configurable
        model.component("comp1").mesh("mesh1").create("size2", "Size");
        model.component("comp1").mesh("mesh1").feature("size2").selection().named("geom1_cyl_sel");
        model.component("comp1").mesh("mesh1").feature("size2").set("custom", "on");
        model.component("comp1").mesh("mesh1").feature("size2").set("hmax", String.valueOf(config.cylinderMeshMaxSize));
        model.component("comp1").mesh("mesh1").feature("size2").set("hmaxactive", true);

        model.component("comp1").mesh("mesh1").create("ftri1", "FreeTri");
        model.component("comp1").mesh("mesh1").run();

        // ============================================
        // 5. Study: Configurable time range
        // ============================================
        model.study().create("std1");
        model.study("std1").create("time", "Transient");
        model.study("std1").feature("time").set("tlist", config.getTimeListString());

        // ============================================
        // 6. Results
        // ============================================
        model.result().create("pg1", "PlotGroup2D");
        model.result("pg1").label("Velocity Magnitude");
        model.result("pg1").create("surf1", "Surface");
        model.result("pg1").feature("surf1").set("expr", "spf.U");
        model.result("pg1").feature("surf1").set("colortable", "RainbowLight");

        model.result().create("pg2", "PlotGroup2D");
        model.result("pg2").label("Vorticity");
        model.result("pg2").create("surf1", "Surface");
        model.result("pg2").feature("surf1").set("expr", "spf.vorticityz");
        model.result("pg2").feature("surf1").set("colortable", "Cyclic");

        model.result().create("pg3", "PlotGroup2D");
        model.result("pg3").label("Pressure");
        model.result("pg3").create("surf1", "Surface");
        model.result("pg3").feature("surf1").set("expr", "p");
        model.result("pg3").feature("surf1").set("colortable", "ThermalWave");

        // Ensure plotting is on for image export
        model.result("pg1").run();
        model.result("pg2").run();
        model.result("pg3").run();

        // ============================================
        // 7. Run and Save
        // ============================================
        System.out.println("Starting simulation (" + config.startTime + "s to " + config.endTime + "s)...");
        try {
            model.study("std1").run();
            System.out.println("Simulation completed!");

            // Save model with configurable filename in fluid-specific subfolder
            String baseDir = config.getEffectiveOutputDir();
            String fluidFolder = config.fluidName.toLowerCase(); // air / water

            // 创建子文件夹（如果不存在）
            java.io.File folder = new java.io.File(baseDir + "/" + fluidFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String modelPath = baseDir + "/" + fluidFolder + "/" + config.modelFileName;
            System.out.println("Saving model to " + modelPath + "...");
            model.save(modelPath);
            System.out.println("Model saved successfully!");

            // ============================================
            // 7.1. Post-processing Setup
            // ============================================
            // Explicitly link results to the generated solution dataset (dset1)
            // This fixes the issue of blank images
            System.out.println("Linking plot groups to solution dataset...");
            model.result("pg1").set("data", "dset1");
            model.result("pg2").set("data", "dset1");
            model.result("pg3").set("data", "dset1");

            // ============================================
            // 8. Export Images and Animation
            // ============================================
            // 使用已创建的流体类型子文件夹
            String validPath = baseDir.replace("\\", "/") + "/" + fluidFolder + "/";

            System.out.println("Output directory: " + validPath);
            System.out.println("Fluid type: " + config.fluidName + " -> folder: " + fluidFolder);

            // Export velocity image if enabled
            if (config.exportVelocity) {
                try {
                    System.out.println("Updating plot group 1 (Velocity)...");
                    model.result("pg1").set("showlegends", true);
                    model.result("pg1").run();

                    System.out.println("Exporting velocity.png to " + validPath + "velocity.png");
                    model.result().export().create("img1", "Image");
                    model.result().export("img1").set("sourceobject", "pg1");
                    model.result().export("img1").set("filename", validPath + "velocity.png");
                    model.result().export("img1").set("zoomextents", "on");
                    model.result().export("img1").set("legend", true);
                    model.result().export("img1").run();
                    System.out.println("SUCCESS: Exported velocity.png");
                } catch (Throwable e) {
                    System.out.println("ERROR: Failed to export velocity.png");
                    System.out.println("Exception message: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            // Export vorticity image if enabled
            if (config.exportVorticity) {
                try {
                    System.out.println("Updating plot group 2 (Vorticity)...");
                    model.result("pg2").set("showlegends", true);
                    model.result("pg2").run();

                    System.out.println("Exporting vorticity.png to " + validPath + "vorticity.png");
                    model.result().export().create("img2", "Image");
                    model.result().export("img2").set("sourceobject", "pg2");
                    model.result().export("img2").set("filename", validPath + "vorticity.png");
                    model.result().export("img2").set("zoomextents", "on");
                    model.result().export("img2").set("legend", true);
                    model.result().export("img2").run();
                    System.out.println("SUCCESS: Exported vorticity.png");
                } catch (Throwable e) {
                    System.out.println("ERROR: Failed to export vorticity.png");
                    System.out.println("Exception message: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            // Export pressure image if enabled
            if (config.exportVorticity) {
                try {
                    System.out.println("Updating plot group 3 (Pressure)...");
                    model.result("pg3").set("showlegends", true);
                    model.result("pg3").run();

                    System.out.println("Exporting pressure.png to " + validPath + "pressure.png");
                    model.result().export().create("img3", "Image");
                    model.result().export("img3").set("sourceobject", "pg3");
                    model.result().export("img3").set("filename", validPath + "pressure.png");
                    model.result().export("img3").set("zoomextents", "on");
                    model.result().export("img3").set("legend", true);
                    model.result().export("img3").run();
                    System.out.println("SUCCESS: Exported pressure.png");
                } catch (Throwable e) {
                    System.out.println("ERROR: Failed to export pressure.png");
                    System.out.println("Exception message: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            // Animation Export - velocity if enabled
            if (config.exportAnimation) {
                try {
                    System.out.println("Exporting velocity_animation.gif...");
                    model.result().export().create("anim0", "Animation");

                    model.result().export("anim0").set("plotgroup", "pg1");
                    model.result().export("anim0").set("target", "file");
                    model.result().export("anim0").set("type", "movie");
                    model.result().export("anim0").set("movietype", "gif");
                    model.result().export("anim0").set("giffilename", validPath + "velocity_animation.gif");
                    model.result().export("anim0").set("framesel", "all"); // 使用所有帧

                    model.result().export("anim0").set("fps", config.animationFps);
                    model.result().export("anim0").set("maxframes", config.animationMaxFrames);

                    model.result().export("anim0").run();
                    System.out.println("SUCCESS: Exported velocity_animation.gif");

                } catch (Throwable e) {
                    System.out.println("ERROR: Failed to export velocity animation");
                    System.out.println("Exception message: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            // Animation Export - vorticity if enabled
            if (config.exportAnimation) {
                try {
                    System.out.println("Exporting vorticity_animation.gif...");
                    model.result().export().create("anim1", "Animation");

                    model.result().export("anim1").set("plotgroup", "pg2");
                    model.result().export("anim1").set("target", "file");
                    model.result().export("anim1").set("type", "movie");
                    model.result().export("anim1").set("movietype", "gif");
                    model.result().export("anim1").set("giffilename", validPath + "vorticity_animation.gif");
                    model.result().export("anim1").set("framesel", "all"); // 使用所有帧

                    model.result().export("anim1").set("fps", config.animationFps);
                    model.result().export("anim1").set("maxframes", config.animationMaxFrames);

                    model.result().export("anim1").run();
                    System.out.println("SUCCESS: Exported vorticity_animation.gif");

                } catch (Throwable e) {
                    System.out.println("ERROR: Failed to export vorticity animation");
                    System.out.println("Exception message: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            // Animation Export - pressure if enabled
            if (config.exportAnimation) {
                try {
                    System.out.println("Exporting pressure_animation.gif...");
                    model.result().export().create("anim2", "Animation");

                    model.result().export("anim2").set("plotgroup", "pg3");
                    model.result().export("anim2").set("target", "file");
                    model.result().export("anim2").set("type", "movie");
                    model.result().export("anim2").set("movietype", "gif");
                    model.result().export("anim2").set("giffilename", validPath + "pressure_animation.gif");
                    model.result().export("anim2").set("framesel", "all"); // 使用所有帧

                    model.result().export("anim2").set("fps", config.animationFps);
                    model.result().export("anim2").set("maxframes", config.animationMaxFrames);

                    model.result().export("anim2").run();
                    System.out.println("SUCCESS: Exported pressure_animation.gif");

                } catch (Throwable e) {
                    System.out.println("ERROR: Failed to export pressure animation");
                    System.out.println("Exception message: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }
        } catch (Exception e) {
            System.out.println("CRITICAL ERROR: Simulation failed!");
            e.printStackTrace(System.out);
        }

        return model;
    }

    /**
     * Helper method to setup boundary conditions based on type
     */
    private static void setupBoundary(Model model, String name, String selectionName, String boundaryType) {
        String featureName = name + "_bc";
        switch (boundaryType) {
            case "Symmetry":
                model.component("comp1").physics("spf").create(featureName, "Symmetry", 1);
                model.component("comp1").physics("spf").feature(featureName).selection().named(selectionName);
                model.component("comp1").physics("spf").feature(featureName).label(name + " Symmetry");
                break;
            case "Wall":
                model.component("comp1").physics("spf").create(featureName, "Wall", 1);
                model.component("comp1").physics("spf").feature(featureName).selection().named(selectionName);
                model.component("comp1").physics("spf").feature(featureName).label(name + " Wall");
                break;
            case "Slip":
                model.component("comp1").physics("spf").create(featureName, "Wall", 1);
                model.component("comp1").physics("spf").feature(featureName).selection().named(selectionName);
                model.component("comp1").physics("spf").feature(featureName).set("BoundaryCondition", "Slip");
                model.component("comp1").physics("spf").feature(featureName).label(name + " Slip");
                break;
            default:
                // Default to Symmetry
                model.component("comp1").physics("spf").create(featureName, "Symmetry", 1);
                model.component("comp1").physics("spf").feature(featureName).selection().named(selectionName);
                model.component("comp1").physics("spf").feature(featureName).label(name + " Boundary");
        }
    }
}
