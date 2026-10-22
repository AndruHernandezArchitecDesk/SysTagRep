package com.vendex.controller;

import com.vendex.model.StockNegativoItem;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StockNegativoPaneTest {
    @BeforeAll static void fx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }
    @FunctionalInterface interface Accion { void ejecutar() throws Exception; }
    private void fxRun(Accion accion) throws Exception {
        var error=new AtomicReference<Throwable>(); var latch=new CountDownLatch(1);
        Platform.runLater(()->{try{accion.ejecutar();}catch(Throwable t){error.set(t);}finally{latch.countDown();}});
        assertTrue(latch.await(10,TimeUnit.SECONDS)); if(error.get()!=null)throw new AssertionError(error.get());
    }
    private void esperar(java.util.function.BooleanSupplier b)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline){ boolean[] listo={false};fxRun(()->listo[0]=b.getAsBoolean());if(listo[0])return;Thread.sleep(10);}
        fail("Timeout del panel de stock");
    }
    @Test void cargaNegativosOrdenadosFechaDescNulosAlFinal_yActualizaManualmente()throws Exception{
        var llamadas=new AtomicInteger();
        var stageRef=new AtomicReference<Stage>();
        int consultasAntes=llamadas.get();
        fxRun(()->{
            var panel=new StockNegativoPane(sucursal->{
                llamadas.incrementAndGet();
                return List.of(new StockNegativoItem(1,"VIEJO","Viejo",-1,"2026-01-01T10:00:00"),
                        new StockNegativoItem(2,"NUEVO","Nuevo",-4,"2026-06-01T10:00:00"),
                        new StockNegativoItem(3,"SIN-FECHA","Sin fecha",-2,null),
                        new StockNegativoItem(4,"POSITIVO","No mostrar",2,"2026-12-01T00:00:00"));
            },()->4,()->17);
            var stage=new Stage();stage.setScene(new Scene(panel,500,500));stageRef.set(stage);stage.show();
        });
        esperar(()->llamadas.get()>=1);
        fxRun(()->{
            var stage=stageRef.get();
            var pane=(StockNegativoPane)stage.getScene().getRoot();
            assertEquals(List.of("NUEVO","VIEJO","SIN-FECHA"),pane.tabla().getItems().stream().map(StockNegativoItem::codigo).toList());
            pane.botonActualizar().fire();
        });
        esperar(()->llamadas.get()>consultasAntes);
        fxRun(()->stageRef.get().close());
    }

    @Test void errorYCambioDeSucursalNoMuestranResultadosDePeticionAnterior()throws Exception{
        AtomicInteger sucursal=new AtomicInteger(2), contador=new AtomicInteger();
        AtomicReference<StockNegativoPane> paneRef=new AtomicReference<>();
        AtomicReference<Stage> stageRef=new AtomicReference<>();
        fxRun(()->{var pane=new StockNegativoPane(s->{
            int n=contador.incrementAndGet();
            if(n==1)throw new java.sql.SQLException("prueba");
            return List.of(new StockNegativoItem(n,"SUC-"+s,"Sucursal " + s,-1,"2026-10-22T00:00:00"));
        },sucursal::get,()->5);paneRef.set(pane);var stage=new Stage();stage.setScene(new Scene(pane,450,400));stageRef.set(stage);stage.show();});
        esperar(()->contador.get()>=1);
        esperar(()->paneRef.get().estado().contains("No se pudo actualizar"));
        fxRun(()->{sucursal.set(3);paneRef.get().actualizar();});
        esperar(()->contador.get()==2);
        fxRun(()->{assertEquals("SUC-3",paneRef.get().tabla().getItems().get(0).codigo());paneRef.get().cancelar();
            assertTrue(paneRef.get().tabla().getItems().isEmpty());stageRef.get().close();});
    }
}
