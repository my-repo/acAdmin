$(document).ready(function(event) {
	APP = new AC.App();
	window.onbeforeunload = function(){
		APP.ws.close();
	};
	$("#GO1").on("click", function(){
		APP.send("Personne", {nom:'Sportes', age:63});
	});
	$("#GON").on("click", function(){
		APP.send("Personnes", [{nom:'Sportes', age:63}, {nom:'Colin', age:62}]);
	});
});

String.prototype.escapeHTML = function() {
	return this.split("&").join("&amp;").split("<").join("&lt;").split(">").join("&gt;").split("\n").join("<br>");
}

$.Class("AC.App", {
	
}, {
	init : function(){
		this.LOG = $("#LOG");
        this.ws = new WebSocket("ws://localhost:8887");
        this.ws.onopen = function() {
            console.log("[WebSocket#onopen]\n");
        }
        this.ws.onmessage = function(e) {
  			var exs = null;
  			var data = null;
      		try {
       	   		data = $.parseJSON(e.data);
       		} catch(ex){ 
       			exs = ex.toString();
       		}
            if (data && (data.type == "log" || data.type == "err"))
            	APP.log(data.data, data.type == "err");
            else {
           		var echo = JSON.stringify(data);
                console.log("[WebSocket#onmessage] : " + echo + "\n");
                $("#RET").html("Echo : " + echo);            	
            }
        }
        this.ws.onclose = function() {
            console.log("[WebSocket#onclose]\n");
            this.ws = null;
            close();
        }
	},
	
	send : function(type, obj){
		var arg = JSON.stringify({type:type, data:obj});
        this.ws.send(arg);
	},
	
	log : function(text, err){
		if (text) {
			var cl = err ? "<div class='ac-fontMedium2B acErr'>" : "<div class='ac-fontMedium2'>";
			this.LOG.append(cl + text.escapeHTML() + "</div>");
			this.LOG.scrollTop(this.LOG[0].scrollHeight);
		}
	}

});