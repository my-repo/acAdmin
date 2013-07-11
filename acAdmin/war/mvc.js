$.Class("AC.MC", {
	// init : function(name, owner, _view)
	// addVar : function(name, group, fcheck, fequal, ignoreEd)
	// addExpr : function(name, depends, fxexpr, fequal)
	// addRich : function(name, val, enable, decor, fac)
	// addInput : function(name, val, enable, fedit, fparse, onCR, fac)
	// addHtml : function(name, onClick, fac)
	// checkAll : function(fcheck)
	// afterCommit : function(fafter)
	
	jqNull : $("#neverfoundsuchathing")
	
}, {
	
	init : function(name, owner, _view){
		if (typeof name != "string")
			throw new Error("AC.MC.init() / name : [" + name + "] is not a valid MC name");
		this.num = 0;
		this.name = name;
		this.owner = owner;
		this.owner.MC = this;
		this.view = _view ? _view : this.owner._content;
		this.todo = null;
		this.vars = {};
		this.groups = {};
		this.views = {};
		this.evlist = [];
		this.error = null;
		this.edited = false;
		this.objError = new AC.Error(this);
		this.objEdited = new AC.Edited(this);
		this.lev = 0;
	},
	
	addVar : function(name, group, fcheck, fequal, ignoreEd){
		var v = new AC.Var(this, name, group, fcheck, fequal, ignoreEd);
		if (!this.groups[group])
			this.groups[group] = [];
		this.groups[group].push(v);
		return this;
	},
	
	addExpr : function(name, depends, fxexpr, fequal){
		new AC.Expr(this, name, depends, fxexpr, fequal);
		return this;
	},
		
	addRich : function(name, val, enable, decor, fac){		
		new AC.Rich(this, name, val, enable, decor, fac);
		return this;
	},
	
	addInput : function(name, val, enable, fedit, fparse, onCR, fac){
		new AC.Input(this, name, val, enable, fedit, fparse, onCR, fac);
		return this;
	},
	
	addHtml : function(name, onClick, fac){
		var v = new AC.Html(this, name, onClick, fac);
		return this;
	},
	
	jqCont : function(name){
		var v = this.views[name];
		return v ? v.view : AC.MC.jqNull;
	},
	
	checkAll : function(fcheck){
		if (fcheck && typeof fcheck != "function")
			throw new Error("AC.MC.checkAll() (" + this.name + ") check : [" + fcheck + "] is not a function");
		this.check = fcheck ? fcheck : null;
		return this;
	},

	afterCommit : function(fafter){
		if (fafter && typeof fafter != "function")
			throw new Error("AC.MC.afterCommit() (" + this.name + ") after : [" + fafter + "] is not a function");
		this.after = fafter ? fafter : null;
		return this;
	},

	hasEdit : function(name){
		var v = this.varExprX(name);
		return (v.type() == "var" && v.edVal != null && !v.ignoreEd);
	},
	
	valEd : function(name){
		var v = this.varExprX(name);
		return v.type() == "var" ? v.edVal : null;
	},
	
	val : function(name){
		return this.varExprX(name).val();
	},
		
	varExprX : function(name){
		var x = this.vars[name];
		if (!x)
			throw new Error("AC.MC.varExpr() (" + this.name + ") [" + name + "] is not a var / expr");
		return x;
	},

	begin : function(){
		this.lev++;
		this.todo = {};
		this.changed = {};
		this.nbEdition = 0;
		if (this.lev == 1)
			for(var x in this.vars)
				if (this.vars[x].type == "var") {
					this.changed[x] = this.vars[x];
					this.nbEdition++;
				}
		this.objError.begin();
		this.objEdited.begin();
		this.edited = false;
		this.error = null;
		return this;
	},
	
	trig : function(v){
		if (v.type() == "var") {
			this.nbEdition++;
			this.changed[v.name] = v;
		}
		if (!v || !v.triggers)
			return;
		for(var i = 0, x = null; x = v.triggers[i]; i++)
			this.todo[x.name] = x;
	},
	
	chtr : function(){
		if (this.todo == null)
			throw new Error("AC.MC.sync() (" + this.name 
				+ ") : this action must be called between begin() and commit()");		
	},
	
	resetEdit : function(group1, group2, group3){
		this.chtr();
		for(var i = 0, gn = null; gn = arguments[i]; i++){
			var g = this.groups[gn];
			if (!g)
				throw new Error("AC.MC.resetEdit() (" + 
					this.name + ") : [" + gn + "] is not a valid group name");
			for(var j = 0, v = null; v = g[j]; j++)
				if (v.setEd(null))
					this.trig(v);
		}
		return this;
	},

	sync : function(group, obj0, obj1, obj2){
		this.chtr();
		var g = this.groups[group];
		if (!g)
			throw new Error("AC.MC.sync() (" + this.name 
				+ ") : [" + group + "] is not a valid group name");
		if (arguments.length == 1)
			return this.resetSrc(g.name);
		for(var j = 0, v = null; v = g[j]; j++){
			var n = v.name;
			var value = null;
			for(var i = 1, obj = null; obj = arguments[i]; i++){
				value = obj[n];
				if (value === undefined)
					continue;
				break;
			}
			if (v.setSrc(value))
				this.trig(v);
		}
		return this;
	},
	
	setSrc : function(name, value){
		this.chtr();
		var v = this.varExprX(name);
		if (v.type() == "var")
			if (v.setSrc(value))
				this.trig(v);
		return this;
	},

	set : function(name, value){
		this.chtr();
		var v = this.varExprX(name);
		if (v.type() == "var")
			if (v.setEd(value))
				this.trig(v);
		return this;
	},

	setError : function(name, error){
		this.chtr();
		if (!error)
			return this;
		var v = this.varExprX(name);
		if (v.type() == "var") {
			v.error = error;
			this.error = error;
			this.trig(v);
		}
		return this;
	},

	resetSrc : function(){
		this.chtr();
		for(var i = 0, gn = null; gn = arguments[i]; i++){
			var g = this.groups[gn];
			if (!g)
				throw new Error("AC.MC.resetEdit() (" + this.name + ") : [" + gn + "] is not a valid group name");
			for(var j = 0, v = null; v = g[j]; j++)
				if (v.setSrc(null))
					this.trig(v);
		}
		return this;
	},
	
	findAllViews : function(){
		for(var v in this.views)
			v.setView();
	},

	commit : function(){
		// check errors
		if (this.nbEdition)
			for (var n in this.changed){
				var v = this.changed[n];
				v.checkMe();
				if (v.error) {
					this.error = v.error;
					break;
				}
			}
		if (!this.error && this.check)
			this.error = this.check.call(this.owner, this.changed);
		for(var vn in this.vars){
			var v = this.vars[vn];
			if (v.type() == "var" && v.edVal != null && !v.ignoreEd) {
				this.edited = true;
				break;
			}
		}
		// eval expr / update views
		for(var j = 0, e = null; e = this.evlist[j]; j++){
			if (!e.lev || this.todo[e.name])
				if (e.evalMe())
					this.trig(e);
		}
		// after 
		if (this.after) {
			var self = this;
			setTimeout(function(){
				self.after.call(self.owner, self.changed);
			}, 5);
		}
		this.todo = null;
		return this;
	}
				
});
/************************************************************/
$.Class("AC.MCView", {
}, {
	init : function(mc, name, fac){
		this.mc = mc;
		this.name = name;
		this.fac = fac;
		if (this.mc.views[name])
			throw new Error("AC.View.init() (" + this.mc.name + ") name : [" + name + "] duplicated name");
		this.mc.views[name] = this;
		this.lev = 0;
		this.depends = []
	},
	
	addDepend : function(name){
		var vx = this.mc.varExprX(name);
		this.depends.push(vx);
		vx.register(this);
		var e = this.mc.evlist;
		var l = e.length;
		if (l == 0 || e[l-1] != this)
			e[l] = this;
		return name;
	},
	
	setView : function(){
		this.view = this.mc.view.find("[data-ac-id='" + this.name + "']");
		if (!this.hasView())
			this.view = this.mc.owner["_" + this.name];
		if (!this.fac && !this.hasView())
			throw new Error("AC.View.setView() (" + 
				this.mc.name + ") name : [" + this.name + "] HTML element not found");
	},
	
	hasView : function(){
		return this.view && this.view.length >= 1;
	}
});
/***********************************************************/
AC.MCView("AC.Rich", {
}, {
	type : function(){
		return "rich";
	},
	
	init : function(mc, name, val, enable, decor, fac){
		this._super(mc, name, fac);
		this.widget = this.mc.owner["_" + name];
		this.disable = enable === false;
		this.val = val ? this.addDepend(val) : null;
		this.enable = !this.disable && enable ? this.addDepend(enable) : null;
		this.decor = decor ? this.addDepend(decor) : null;
		this.setView();
	},
	
	setView : function(){
		this.view = this.widget.jqCont;
		if (this.val && !this.disable){
			var self = this;
			this.view.off("dataentry").on("dataentry", function(event){
				APP.NOPROPAG(event);
				self.mc.begin().set(self.val, self.widget.val()).commit();
			});
		}
	},
	
	evalMe : function(){
		if (this.decor) {
			var v = this.mc.vars[this.decor];
			if (v.lev > this.lev)
				this.widget.setDecor(v.val());
		}
		if (this.val) {
			var v = this.mc.vars[this.val];
			if (v.lev > this.lev && this.widget.val)
				this.widget.val(v.val());
		}
		if (this.enable) {
			var v = this.mc.vars[this.enable];
			if (v.lev > this.lev)
				this.widget.enable(v.val());
		}
		this.lev = this.mc.lev;
		return false;
	}
		
});

/************************************************************/
AC.MCView("AC.Input", {
}, {
	type : function() {
		return "input";
	},
	
	init : function(mc, name, val, enable, edit, parse, onCR, fac){
		this.lastVal = "";
		this.lastStatus = 0; // 0:OK, 1:ed 2:er
		this.disable = enable === false;
		this.onCr = null;
		if (onCR) {
			if (typeof onCR != "function")
				throw new Error("AC.Input.init() (" + this.mc.name + " / " + this.name + ") onCR : [" + onCR + "] is not a function");
			this.onCR = onCR;
		}
		this.edit = null;
		if (edit) {
			if (typeof edit != "function")
				throw new Error("AC.Input.init() (" + this.mc.name + " / " + this.name + ") edit : [" + edit + "] is not a function");
			this.edit = edit;
		}
		this.parse = null;
		if (parse) {
			if (typeof parse != "function")
				throw new Error("AC.Input.init() (" + this.mc.name + " / " + this.name + ") parse : [" + parse + "] is not a function");
			this.parse = parse;
		}
		this._super(mc, name, fac);
		this.val = val ? this.addDepend(val) : null;
		this.enable = !this.disable && enable ? this.addDepend(enable) : null;
		this.setView();
		if (this.val)
			this.view.val("");
	},
	
	setView : function(){
		this._super();
		this.isTA = this.hasView() && this.view[0].tagName == "TEXTAREA";
		if (this.val && !this.disable){
			var self = this;
			this.view.off(APP.KEYUP).on(APP.KEYUP, function(event){
				APP.NOPROPAG(event);
				if (self.onCR && event.keyCode == 13)
					self.onCR.call(self.mc.owner);
				else
					self.process();
			});
		}
	},
	
	process : function(){
		var val = this.view.val();
		if (this.parse)
			val = this.parse.call(this.mc.owner, val);
		var v = this.mc.vars[this.val];
		if (v.srcVal == null && val === "")
			val = null;
		if (val === this.lastVal)
			return;
		this.lastVal = val;
		this.mc.begin().set(this.val, val).commit();
	},
	
	evalMe : function() {
		if (this.enable) {
			var v = this.mc.vars[this.enable];
			if (v.lev > this.lev)
				Util.btnEnable(this.view, v.val());
		}
		if (!this.val) {
			this.lev = this.mc.lev;
			return false;
		}
		var v = this.mc.vars[this.val];
		if (v.lev <= this.lev) {
			this.lev = this.mc.lev;
			return false;
		}
		this.lev = this.mc.lev;
		var val = v.val();
		if (!(this.lastVal === val)) {
			if (this.edit)
				val = this.edit.call(this.mc.owner, val);
			this.view.val(val);
		}
		var p = this.isTA ? this.view : this.view.parent();
		if (v.hasError()) {
			if (this.lastStatus == 1)
				p.removeClass("acEdited");
			if (this.lastStatus != 2)
				p.addClass("acError");
			this.lastStatus = 2;
			return false;
		}
		if (v.isEdited()){
			if (this.lastStatus == 2)
				p.removeClass("acError");
			if (this.lastStatus != 1)
				p.addClass("acEdited");
			this.lastStatus = 1;
			return false;
		}
		if (this.lastStatus == 1)
			p.removeClass("acEdited");
		if (this.lastStatus == 2)
			p.removeClass("acError");
		this.lastStatus = 0;
		
		return false;
	},
		
});

/************************************************************/
AC.MCView("AC.Html", {
}, {
	type : function(){
		return "html";
	},
	
	init : function(mc, name, onClick, fac){
		this.onClick = null;
		if (onClick) {
			if (typeof onClick != "function")
				throw new Error("AC.Html.init() (" + this.mc.name + " / " + this.name + ") onCR : [" + onClick + "] is not a function");
				this.onClick = onClick;
		}
		this._super(mc, name, fac);
		this.setView();
	},
	
	setView : function(){
		this._super();
		if (this.onClick){
			var self = this;
			this.view.off(APP.CLICK).on(APP.CLICK, function(event){
				APP.NOPROPAG(event);
				self.onClick.call(self.mc.owner);
			});
		}
	}
	
});

/************************************************************/
$.Class("AC.MCCell", {

}, {
	init : function(mc, name, depends){
		this.mc = mc;
		this.name = name;
		this.lev = 0;
		if (typeof name != "string")
			throw new Error("AC.MCCell.init() (" 
				+ this.mc.name + ") name : [" + name + "] is not a valid var / expr name");
		if (this.mc.vars[name])
			throw new Error("AC.MCCell.init() (" 
				+ this.mc.name + ") name : [" + name + "] duplicated name");
		this.mc.vars[name] = this;
		this.depends = [];
		this.triggers = [];
		var scr = ["var mc=this.MC"]
		if (depends != null){
			if (typeof depends != "object")
				throw new Error("AC.MCCell.init() (" + this.mc.name + " / " + name + ") : expr [" 
					+ depends + "] is not a valid depends object");
			for(var v in depends){
				var d = depends[v];
				if (v == "now"){
					this.timeDepend = true;
					scr.push("APP.Ctx.getTime();var " + d + "=APP.Ctx.now");
					continue;
				}
				if (d.startsWith("_")) {
					d = d.substring(1);
					if (!this.mc.views[d])
						this.mc.addHtml(d, null, true);
					scr.push("var " + v + "=mc.views." + d + ".view");
					continue;
				}
				var dv = this.mc.vars[d];
				if (!dv)
					throw new Error("AC.MCCell.init() (" 
						+ this.mc.name + ") : " + name + " depends on : [" + d + "] not found");
				scr.push("var " + v + "=mc.vars." + d + ".val()");
				dv.register(this);
				this.depends.push(dv);
			}
		}
		this.script = scr.join(";") + ";";
		if (this.depends.length != 0)
			this.mc.evlist.push(this);
	},
	
	register : function(aDepend){
		if (this.triggers.indexOf(aDepend) == -1)
			this.triggers.push(aDepend);
	}
});
/************************************************************/
AC.MCCell("AC.Var", {
}, {
	type : function(){
		return "var";
	},

	init : function(mc, name, group, check, equal, ignoreEd){
		if (typeof group != "string")
			throw new Error("AC.Var.init() (" + this.mc.name + ") group : [" + group + "] is not a valid group name");
		if (equal && typeof equal != "function")
			throw new Error("AC.Var.init() (" + this.mc.name + ") equal : [" + equal + "] is not a function");
		if (check && typeof check != "function")
			throw new Error("AC.Var.init() (" + this.mc.name + ") check : [" + check + "] is not a function");
		this.group = group;
		this.equal = equal ? equal : null;
		this.check = check ? check : null;
		this.srcVal = null;
		this.edVal = null;
		this.error = null;
		this.ignoreEd = ignoreEd;
		this._super(mc, name);
	},
		
	isVar : function(){
		return true;
	},
	
	val : function(){
		return this.isEdited() ? this.edVal : this.srcVal;
	},
	
	isEdited : function(){
		return !(this.edVal === undefined || this.edVal == null) ? true : false;
	},

	hasError : function(){
		return this.error ? true : false;
	},

	setSrc : function(v){
		this.error = null;
		var old = this.val();
		if (v === undefined || v == null) {
			this.srcVal = null;
			if (old != null) {
				this.lev = this.mc.lev;
				return true;
			} else
				return false;			
		}
		this.srcVal = v;
		if (this.isEqual(this.srcVal, this.edVal))
			this.edVal = null;
		if (!this.isEqual(old, this.val())) {
			this.lev = this.mc.lev;
			return true;
		}
		return false;
	},
	
	setEd : function(v){
		this.error = null;
		var old = this.val();
		if (v === undefined || v == null)
			this.edVal = null;
		else
			this.edVal = this.isEqual(this.srcVal, v) ? null : v;
		if (!this.isEqual(old, this.val())) {
			this.lev = this.mc.lev;
			return true;
		}
		return false;		
	},
	
	isEqual : function(a, b){
		if (!this.equal)
			return a === b;
		else
			return this.equal(a, b);
	},
	
	checkMe : function(){
		if (this.check)
			this.error = this.check.call(this.mc.owner, this.val(), this);
		return this.error;
	}
	
});
/************************************************************/
AC.MCCell("AC.Expr", {
}, {
	type : function(){
		return "expr";
	},

	init : function(mc, name, depends, expr, equal){
		if (!name)
			name = "E" + mc.num++;
		this.lastValue = null;
		if (equal && typeof equal != "function")
			throw new Error("AC.Expr.init() (" + this.mc.name + " / " + name + ") equal : [" 
				+ equal + "] is not a function");
		this.equal = equal ? equal : null;
		if (typeof expr == "string"){
			this.expr = expr;
			this.func = this.myEval;
		} else if (typeof expr == "function"){
			this.expr = null;
			this.func = expr;
		} else
			throw new Error("AC.Expr.init() (" + this.mc.name + " / " + name + ") expr : [" 
				+ expr + "] is not an expression and not a function");
		this._super(mc, name, depends);
		if (this.expr)
			this.script += this.expr + ";";
	},
		
	isEdited : function(){
		return false;
	},

	hasError : function(){
		return false;
	},

	val : function(){
		return this.lastValue;
	},

	myEval : function(scr){
		return eval(scr);
	},
	
	evalMe : function(){
		var old = this.lastValue;
		try {
			this.lastValue = this.func.call(this.mc.owner, this.script);
		} catch (e) {
			var msg = "AC.Expr computeMe (" + this.mc.name + " / " + this.name 
				+ ") : eval exception [" + e.message + "]";
			if (APP.debug)
				console.log(msg + "\n" + this.script);
			throw new Error(msg);
		}
		if (!this.timeDepend)
			this.lev = this.mc.lev;
		return !this.isEqual(old, this.lastValue);
	},
	
	isEqual : function(a, b){
		if (!this.equal)
			return a === b;
		else
			return this.equal(a, b);
	}
	
});
/************************************************************/
AC.MCCell("AC.Error", {
}, {
	type : function(){
		return "expr";
	},

	init : function(mc){
		this.old = "";
		this._super(mc, "error");
		this.mc.evlist.push(this);
	},
	
	begin : function(){
		this.old = this.val();
	},
	
	val : function(){
		return this.mc.error ? this.mc.error : "";
	},

	isEqual : function(a, b){
		return a === b;
	},

	evalMe : function(){
		return this.old != this.val();
	}
	
});
/************************************************************/
AC.MCCell("AC.Edited", {
}, {
	type : function(){
		return "expr";
	},

	init : function(mc){
		this.old = "";
		this._super(mc, "edited");
		this.mc.evlist.push(this);
	},
	
	begin : function(){
		this.old = this.val();
	},
	
	val : function(){
		return this.mc.edited;
	},

	isEqual : function(a, b){
		return a === b;
	},
	
	evalMe : function(){
		return this.old != this.val();
	}
	
});
