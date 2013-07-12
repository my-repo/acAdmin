$(document).ready(function(event) {
	APP = new AC.App();
	APP.debug = true;
	window.onbeforeunload = function(){
		APP.ws.close();
	};
	new AC.Main();
});

/** **************************************************************** */

String.prototype.escapeHTML = function() {
	return this.split("&").join("&amp;").split("<").join("&lt;").split(">").join("&gt;").split("\n").join("<br>");
}

/** **************************************************************** */

$.Class("AC.HB", {
	param : function(target){
		return target ? target.data("param") : null;
	}
}, {
	//
	init : function() {
		this.t = [];
		this.np = 1;
		this.params = {};
	},

	append : function(text) {
		this.t.push(text);
		return this;
	},

	prepend : function(text) {
		this.t.unshift(text);
		return this;
	},

	param : function(obj) {
		if (!obj)
			return " ";
		this.params["x-" + this.np] = obj;
		return " data-param='x-" + (this.np++) + "' ";
	},

	asString : function(sep) {
		return this.t.join(sep ? sep : "");
	},

	flush : function(jqCont) {
		var html = this.t.join("");
		jqCont.html(html);
		var self = this;
		jqCont.find("[data-param|='x']").each(function() {
			var x = $(this);
			var obj = self.params[x.attr("data-param")];
			x.data("param", obj);
		});
		return jqCont;
	}

});

/** ******************************************************** */
$.Class("AC.PopForm", {
	//
	currentForm : null,
	isOpen : false,
	html1 : "<div class='acBtnFermer' id='fermer'></div>"
		+ "<div id='title' class='acPopFormTitle ac-fontLargeBI'></div>"
		+ "<div id='innerDiv' class='acPopFormDiv ac-fontMedium'>",
	html2 : "</div><div class='acBtn acBtnValider ac-fontLargeBI' id='valider'>"

}, {

	init : function(hb, title, valider) {
		if (!this.constructor._acPopupForm) {
			this.constructor._acPopupForm = $("#acPopupForm");
			this.constructor._acMask = $("#acMask");
		}
		var _content = this.constructor._acPopupForm;
		_content.css("z-index", 202);
		this._content = _content;
		hb.prepend(this.constructor.html1);
		if (valider)
			hb.append(this.constructor.html2).append(valider).append("</div>");
		else
			hb.append("</div>");
		hb.flush(_content);
		if (title)
			_content.find("#title").html(title.escapeHTML());
		if (!this.constructor.currentForm) {
			this.constructor._acMask.css("display", "block");
			var y = APP._work.offset().top;
			_content.css("top", "" + y + "px");
			APP.opacity0(_content);
			_content.css("display", "block");
			setTimeout(function(){
				APP.opacity1(_content);
			}, 30);
		}
		this.constructor.currentForm = this;
		this._innerDiv = _content.find("#innerDiv");
		this._valider = _content.find("#valider");
		APP.oncl(this, "fermer", this.close);
		APP.oncl(this, 	this.constructor._acMask);
	},
		
	repaint : function(t){
		t.flush(this._innerDiv, true);
	},
	
	close : function() {
		var _content = this.constructor._acPopupForm;
		var _mask = this.constructor._acMask;
		APP.opacity0(_content);
		setTimeout(function(){
			_content.html("");
			_content.css("display", "none");
			_mask.css("display", "none");
		}, 300);
		this.constructor.currentForm = null;
	},
		
	enEdition : function() {
		return false;
	},
	
	enErreur : function() {
		return this.error;
	},
		
	enable : function(){
		APP.btnEnable(this._valider, this.enEdition() && !this.enErreur());
	}
	
});

/** ******************************************************** */

$.Class("AC.App", {
	
}, {
	init : function(){
		this.registered = {};
		this.callId = 0;
		
		this._maskA = $("#acMaskA");
		this._attente = $("#acAttente");
		this._work = $("#acWork");
		this.LOG = $("#LOG");
		
		var port = window.location.search.substring(1);
        this.ws = new WebSocket("ws://localhost:" + port);
        this.ws.onopen = function() {
        	if (APP.debug)
        		console.log("[WebSocket#onopen]\n");
        	APP.log("Prêt !");
        }
        this.ws.onmessage = function(e) {
  			var exs = null;
  			var msg = null;
      		try {
       	   		msg = $.parseJSON(e.data);
       		} catch(ex){ 
       			exs = ex.toString();
       		}
            if (msg && (msg.type == "log" || msg.type == "err"))
            	APP.log(msg.data, msg.type == "err");
            else {
           		if (APP.debug)
           			console.log("[WebSocket#onmessage] : " + JSON.stringify(msg) + "\n");
           		if (msg.data && msg.data.callId){
           			if (!(msg.data.callId === APP.callId) || !APP.rpcCb)
           				return;
           			APP.unmaskScreen();
           			var cb = APP.rpcCb;
           			APP.rpcCb = null;
           			if (msg.type == "ErrMsg"){
           				APP.log(msg.data.message, true);
       					cb(1, msg.data.message);
           			} else
           				cb(0, msg.data);
           		} else {
	           		var handler = APP.registered[data.type];
	           		if (handler)
	           			handler.onmessage(data);
	           	}
            }
        }
        this.ws.onclose = function() {
        	if (APP.debug)
        		console.log("[WebSocket#onclose]\n");
            this.ws = null;
            close();
        }
	},
	
	register : function(type, handler){
		if (type)
			this.registered[type] = handler;
	},
	
	send : function(type, obj, cb){
		if (cb){
			this.callId++;
			obj.callId = this.callId;
			this.rpcCb = cb;
			this.maskScreen();
		} else
			this.rpcCb = null;
		var arg = JSON.stringify({type:type, data:obj});
        this.ws.send(arg);
	},
		
	maskScreen : function() {
		this._attente.css("display", "none");
		this.attente = false;
		this._maskA.css("display", "block");
		var self = this;
		this.timerAttente = setTimeout(function() {
			self.timerAttente = null;
			self.attente = true;
			var h = $(window).height();
			var w = $(window).width();
			self._attente.css("top", "" + Math.floor((h - 110) / 2) + "px");
			self._attente.css("left", "" + Math.floor((w - 170) / 2) + "px");
			self.opacity0(APP._attente);
			self._attente.css("display", "block");
			self.oncl(self, APP._attente, function(){
				APP.unmaskScreen();
				var cb = APP.rpcCb;
				if (cb) {
					APP.rpcCb = null;
					cb(-1);
				}
			});
			setTimeout(function() {
				self.opacity07(APP._attente);
			}, 20);
		}, 500);
	},

	unmaskScreen : function() {
		if (this.timerAttente) {
			clearTimeout(this.timerAttente);
			this.timerAttente = null;
		}
		if (this.attente) {
			this.attente = false;
			this.opacity0(this._attente);
			var self = this;
			setTimeout(function() {
				self._attente.css("display", "none");
			}, 200);
		}
		this._maskA.css("display", "none");
	},

	log : function(text, err){
		if (text) {
			var cl = err ? "<div class='ac-fontMedium2B acErr'>" : "<div class='ac-fontMedium2'>";
			this.LOG.append(cl + text.escapeHTML() + "</div>");
			this.LOG.scrollTop(this.LOG[0].scrollHeight);
		}
	},
	
	getScroll : function() {
		var x = 0, y = 0;
		if (typeof (window.pageYOffset) == 'number') {
			// Netscape compliant
			y = window.pageYOffset;
			x = window.pageXOffset;
		} else if (document.body && (document.body.scrollLeft || document.body.scrollTop)) {
			// DOM compliant
			y = document.body.scrollTop;
			x = document.body.scrollLeft;
		} else if (document.documentElement
				&& (document.documentElement.scrollLeft || document.documentElement.scrollTop)) {
			// IE6 standards compliant mode
			y = document.documentElement.scrollTop;
			x = document.documentElement.scrollLeft;
		}
		var obj = new Object();
		obj.x = x;
		obj.y = y;
		return obj;
	},

	CLICK : "click",
	
	KEYUP : "keyup input",
	
	NOPROPAG : function(event) {
		event.stopPropagation();
	},

	oncl : function(caller, id, cb){
		var idx = typeof id == "string" ? caller._content.find("#" + id) : id;
		idx.off(this.CLICK).on(this.CLICK, {cb:cb, caller:caller}, function(event){
			APP.NOPROPAG(event);
			if (event.data.cb)
				event.data.cb.call(event.data.caller, $(event.currentTarget), event);
		});			
	},

	opacity1 : function(target){
		target.css("opacity", 1);
		target.css("filter", "alpha(opacity=100)")
	},

	opacity0 : function(target){
		target.css("opacity", 0);
		target.css("filter", "alpha(opacity=0)")
	},

	opacity07 : function(target){
		target.css("opacity", 0.7);
		target.css("filter", "alpha(opacity=70)")
	},

	btnEnable : function(cmp, enable) {
		if (!cmp)
			return;
		if (enable)
			cmp.removeClass('ui-disabled');
		else
			cmp.addClass('ui-disabled');
	},

});

/*******************************************************/
AC.PopForm("AC.Test", {
	html : "<div class='ac-fontMediumB acBtn' id='GO1'>GO-une</div>"
		+ "<div class='ac-fontMediumB acBtn' id='GON'>GO-liste</div>"
		
}, {
	init : function(){
		var hb = new AC.HB();
		hb.append(this.constructor.html);
		this._super(hb, "Test PopForm");
		APP.oncl(this, "GO1", function(target){
    		APP.send("Personne", {nom:'Sportes', age:63});
    	});
		APP.oncl(this, "GON", function(){
    		APP.send("Personnes", [{nom:'Sportes', age:63}, {nom:'Colin', age:62}]);
    	});
		APP.register("Personne", this);
		APP.register("Personnes", this);
	},
	
	onmessage : function(data){
		var x = JSON.stringify(data);
		APP.log(x, data.type == "Personne");
	},
	
	close : function(){
		APP.register("Personne");
		APP.register("Personnes");
		this._super();
	}
});

/*******************************************************/
AC.PopForm("AC.Config", {
	html : "<div class='ac-fontMediumB acBtn acFLR' id='syncBtn'>Sync</div>"
		+ "<div class='ac-fontMedium' id='dir'></div>"
		+ "<div><div class='acNewdirInp'><input type='text' id='newdirInp'></input></div>"
		+ "<div class='ac-fontMediumB acBtn' id='newdirBtn'>Nouveau</div></div>"
		+ "<div class='ac-fontMedium' id='subdirs'></div>"
		
}, {
	init : function(){
		var hb = new AC.HB();
		hb.append(this.constructor.html);
		this._super(hb, "Configuration");
		this.reload();
		APP.oncl(this, "syncBtn", this.reload);
		this._newdirInp = this._content.find("#newdirInp");
		this._newdirBtn = this._content.find("#newdirBtn");
		APP.oncl(this, this._newdirBtn, this.newdir);
		var self = this;
		this._newdirInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			APP.NOPROPAG(event);
			if (event.keyCode == 13)
				self.newdir();
			else {
				self.newdirName = self._newdirInp.val();
				APP.btnEnable(self._newdirBtn, self.newdirName);
			}
		});
	},
	
	newdir : function(){
		var self = this;
		APP.send("UpdConfigNewDir", {newdir:this.newdirName}, function(err, data){
			if (!err) {
				self.newdirName = "";
				self._newdirInp.val("");
				APP.btnEnable(self._newdirBtn, false);
				self.display(data);
			}
		});
	},
	
	reload : function(){
		var self = this;
		APP.send("ReqConfig", {}, function(err, data){
			if (!err)
				self.display(data);
		});
	},
	
	getSubDirs : function(){
		var self = this;
		APP.send("ReqConfigDirs", {}, function(err, data){
			if (!err)
				self.displayDirs(data);
			else
				self.displayDirs({dirs:[]});
		});	
	},
	
	updConfigDir : function(dir){
		var self = this;
		APP.send("UpdConfigDir", {dir:dir}, function(err, data){
			if (!err)
				self.display(data);
		});			
	},
	
	display : function(data){
		this.config = data;
		var t = new AC.HB();
		t.append("<div class='acPath'>");
		for(var i = 0; i < data.dir.length; i++){
			if (i)
				t.append("<div class='acDirsep ac-fontMedium2'>/</div>");
			t.append("<div class='acDirname ac-fontMedium2B' data-index='" + i + "'>" + data.dir[i] + "</div>");
		}
		t.append("</div>");
		t.flush(this._content.find("#dir"));
		APP.oncl(this, this._content.find(".acDirname"), function(target){
			var n = parseInt(target.attr("data-index"), 10);
			this.updConfigDir(data.dir.slice(0, n + 1));
		});
		this.getSubDirs();
	},

	displayDirs : function(data){
		this.subdirs = data.dirs;
		var t = new AC.HB();
		for(var i = 0; i < data.dirs.length; i++){
			t.append("<div class='acDirname2 ac-fontMedium2B' data-index='" + i + "'>" + data.dirs[i] + "</div>");
		}
		t.flush(this._content.find("#subdirs"));
		APP.oncl(this, this._content.find(".acDirname2"), function(target){
			var n = parseInt(target.attr("data-index"), 10);
			var nd = [];
			for(var i = 0, d = null; d = this.config.dir[i]; i++)
				nd.push(d);
			nd.push(this.subdirs[n]);
			this.updConfigDir(nd);
		});
	},

	onmessage : function(data){
	},
	
	close : function(){
		this._super();
	}
});

/*******************************************************/
$.Class("AC.Main", {
	
}, {
	init : function(){
		this._content = $("body");
		APP.oncl(this, "configuration", function(){
			new AC.Config();
		});
		APP.oncl(this, "test", function(){
			new AC.Test();
		});
		APP.log("Initialisation ...");
	}
	
});