/*
 * Async Treeview 0.1 - Lazy-loading extension for Treeview
 *
 * http://bassistance.de/jquery-plugins/jquery-plugin-treeview/
 *
 * Copyright (c) 2007 Jörn Zaefferer
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * Revision: $Id$
 *
 */
 /*
 * Copyright 2010 Polopoly AB (publ).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

mbean = function(response) {
    log("MBean: " + response.attributes.MBeanServerId);
    var ret = [];
    $.each(response.attributes, function(attrName, attr) {
        log("Attr " + attr.name);
        var valueNode = attr.writable ?
            "<input class='attribute' type='text' name='" +
            encodeURIComponent(response.name) + "/" +
            encodeURIComponent(attr.name) + "' value='" + attr.value +
            "'/><a class='attribute' href=''><img src='Action-db-update-icon.png'></a></input>" : attr.value;
        ret[ret.length] = {"text": attr.name + " : " + valueNode,
		           "expanded": false,
		           "hasChildren": false,
		           "id": attr,
                           "type": "mbean"};
	     //    log(ret);
	});
    $.each(response.operations, function(index, op) {
        log("Oper " + op.op);
        var operNode = "<span>";
        $.each(op.params, function(i, p) {
            operNode += "<input type='text'>";
        });
        operNode += "</span>";
        operNode += "<a class='operation' name='" +
            encodeURIComponent(response.name) + "/ops/" +
            encodeURIComponent(op.name) +
            "' href=''><img src='Action-db-update-icon.png'></a>";
        ret[ret.length] = {"text": op.operation + " : " + operNode,
		           "expanded": false,
		           "hasChildren": false,
		           "id": op.operation,
                           "type": "mbean"};

    });
    return ret;
}

mbeans = function(response) {
    //    log(response);

    return $.map(response.mbeans, function(mbean, i) {
	    return {"text": mbean,
		    "expanded": false,
		    "hasChildren": true,
		    "id": mbean,
		    "type": "mbeans"};
	});
}

tranfn = function(response) {
    //    log(response);

    return $.map(response, function(domain, i) {
	    return {"text": domain,
		    "expanded": false,
		    "hasChildren": true,
		    "id": domain,
		    "type": "domains"};
	});
}

;(function($) {

function load(settings, root, child, container) {
    log(settings.url);
    $.getJSON(settings.url, {root: root}, function(response) {
	//		log(root);
	if (settings.treeTransform) {
	    response = settings.treeTransform.call(this, response);
	}
		//		log(response);
        function createNode(parent) {
	    var current = $("<li/>").attr("id", this.id || "").html("<span>" + this.text + "</span>").appendTo(parent);
	    if (this.classes) {
		current.children("span").addClass(this.classes);
	    }
	    if (this.expanded) {
		current.addClass("open");
	    }
	    //			log(this.type);
	    current.addClass(this.type);
	    if (this.hasChildren || this.children && this.children.length) {
		var branch = $("<ul/>").appendTo(current);
		if (this.hasChildren) {
		    current.addClass("hasChildren");
		    createNode.call({
			text:"placeholder",
			id:"placeholder",
			children:[]
		    }, branch);
		}
		if (this.children && this.children.length) {
		    $.each(this.children, createNode, [branch])
			}
	    }
	}

	$.each(response, createNode, [child]);
        $(container).treeview({add: child});}
             );
}

var proxied = $.fn.treeview;
$.fn.treeview = function(settings) {
	if (!settings.url) {
		return proxied.apply(this, arguments);
	}
	var container = this;
	load(settings, "source", this, container);
	var userToggle = settings.toggle;
	return proxied.call(this, $.extend({}, settings, {
		collapsed: true,
		toggle: function() {
			var $this = $(this);
			if($this.hasClass("domains")) {
			    settings.url = BASE+"domains/" + encodeURIComponent(this.id) + "?callback=?",
			    settings.treeTransform = mbeans;
			}
			if($this.hasClass("mbeans")) {
			    settings.url = BASE + encodeURIComponent(this.id) + "?callback=?",
			    settings.treeTransform = mbean;
			}
			if ($this.hasClass("hasChildren")) {
				var childList = $this.removeClass("hasChildren").find("ul");
				childList.empty();
				load(settings, this.id, childList, container);
			}
			if (userToggle) {
				userToggle.apply(this, arguments);
			}
		}
	}));
};

})(jQuery);