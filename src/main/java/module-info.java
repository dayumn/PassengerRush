module MPPProject {
	requires javafx.controls;
	requires javafx.graphics;
	
	opens com.cmsc22.views to javafx.graphics, javafx.fxml;
    exports com.cmsc22.views;
    exports com.cmsc22.models;
    exports com.cmsc22.controllers;
}
